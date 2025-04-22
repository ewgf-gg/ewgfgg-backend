package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;
import org.ewgf.utils.DateTimeUtils;
import org.ewgf.utils.EventPublisherUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.ewgf.configuration.BackpressureManager;
import org.ewgf.configuration.RabbitMQConfig;
import org.ewgf.models.Battle;
import org.ewgf.repositories.BattleRepository;
import org.ewgf.repositories.TekkenStatsSummaryRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.ewgf.utils.Constants.TIMESTAMP_HEADER;

@Slf4j
@Service
public class WavuService implements InitializingBean, DisposableBean {

    private static final int DEFAULT_FETCH_INTERVAL_MILLIS = 300;
    private static final int BACKPRESSURE_CHECK_DELAY_MILLIS = 60000; // 1 minute
    private static final int NEW_REPLAYS_DELAY_MILLIS = 30000;// 30 seconds
    private static final int NEW_REPLAYS_DELAY_SECONDS = 30;
    private static final int TIME_STEP = 700;
    private static final int TIME_STEP_OVERLAP = 60; // Overlap to ensure no battles are missed
    private static long OLDEST_HISTORICAL_TIMESTAMP = 1711548580L;

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;
    private final BackpressureManager backpressureManager;
    private final RestTemplate restTemplate;
    private final BattleRepository battleRepository;
    private final TaskScheduler taskScheduler;
    private final TekkenStatsSummaryRepository tekkenStatsSummaryRepository;
    private final EventPublisherUtils eventPublisherUtils;

    private ScheduledFuture<?> scheduledTask;
    private boolean isFetchingNewReplays = false;
    private long currentFetchTimestamp;
    private long newestBattleTimestampInDatabase;
    private boolean currentFetchIsBelowNewestBattleInDatabase = false;

    @Value("${wavu.api}")
    private String wavuApiUrl;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public WavuService(
            RabbitTemplate rabbitTemplate,
            BackpressureManager backpressureManager,
            RestTemplate restTemplate,
            BattleRepository battleRepository,
            TaskScheduler taskScheduler,
            TekkenStatsSummaryRepository tekkenStatsSummaryRepository,
            RabbitMQConfig rabbitMQConfig,
            EventPublisherUtils eventPublisherUtils
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.backpressureManager = backpressureManager;
        this.restTemplate = restTemplate;
        this.battleRepository = battleRepository;
        this.taskScheduler = taskScheduler;
        this.tekkenStatsSummaryRepository = tekkenStatsSummaryRepository;
        this.rabbitMQConfig = rabbitMQConfig;
        this.eventPublisherUtils = eventPublisherUtils;
    }

    @Override
    public void afterPropertiesSet() {
        initializeService();
        scheduleNextExecution(0); // Start immediately
    }

    @Override
    public void destroy() {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
    }

    private void initializeService() {
        try {
            Optional<Battle> oldestBattleInDatabase = battleRepository.findOldestRankedBattle();
            Optional<Battle> newestBattleInDatabase = battleRepository.findNewestRankedBattle();
            checkIfActiveProfileIsDev(activeProfile);

            if (oldestBattleInDatabase.isPresent() && newestBattleInDatabase.isPresent() && isDatabaseFullyPreloaded(oldestBattleInDatabase)) {
                initializeForPreloadedDatabase(newestBattleInDatabase.get());
            } else if (oldestBattleInDatabase.isPresent()) {
                initializeForPartiallyLoadedDatabase(oldestBattleInDatabase.get());
            } else {
                initializeForEmptyDatabase();
            }
        } catch (Exception e) {
            log.error("Error initializing WavuService: {}", e.getMessage());
            System.exit(-1);
        }
    }

    private void fetchReplays() {
        if (handleBackpressure()) return;

        if (isFetchingNewReplays) {
            fetchNewReplays();

            if (currentFetchIsBelowNewestBattleInDatabase) {
                resetFetchStateForNewReplays();
                return;
            }
        } else {
            fetchHistoricalReplays();
        }
        scheduleNextExecution(DEFAULT_FETCH_INTERVAL_MILLIS);
    }

    private boolean handleBackpressure() {
        if (backpressureManager.isBackpressureActive()) {
            if (backpressureManager.isManuallyActivated()) {
                log.warn("MESSAGE RETRIEVAL HAS BEEN MANUALLY PAUSED.");
                scheduleNextExecution(BACKPRESSURE_CHECK_DELAY_MILLIS);
                return true;
            }
            log.warn("BACKPRESSURE ACTIVE: MESSAGE RETRIEVAL IS STOPPED");
            scheduleNextExecution(1200L * backpressureManager.getSlowdownFactor());
            return true;
        }
        return false;
    }

    private void fetchNewReplays() {
        checkIfFetchTimestampBelowNewestBattle();
        String readableTimestamp = DateTimeUtils.toReadableTime(currentFetchTimestamp);
        log.info("Fetching battles before timestamp: {} UTC, Unix: {}", readableTimestamp, currentFetchTimestamp);

        try {
            List<Battle> response = fetchBattlesFromApi(currentFetchTimestamp);
            processApiResponse(response, readableTimestamp);
            currentFetchTimestamp -= (TIME_STEP - TIME_STEP_OVERLAP);
        } catch (Exception e) {
            log.error("Error fetching new replays: {}", e.getMessage());
        }
    }

    private void checkIfFetchTimestampBelowNewestBattle() {
        if (currentFetchTimestamp < newestBattleTimestampInDatabase) {
            log.info("Current fetch timestamp {} ({}) is below newest database timestamp {} ({})",
                    currentFetchTimestamp,
                    DateTimeUtils.toReadableTime(currentFetchTimestamp),
                    newestBattleTimestampInDatabase,
                    DateTimeUtils.toReadableTime(newestBattleTimestampInDatabase));
            currentFetchIsBelowNewestBattleInDatabase = true;
        }
    }

    private void fetchHistoricalReplays() {
        try {
            String readableTimestamp = DateTimeUtils.toReadableTime(currentFetchTimestamp);
            log.info("Fetching historical battles before: {} UTC (Unix: {})", readableTimestamp, currentFetchTimestamp);

            List<Battle> response = fetchBattlesFromApi(currentFetchTimestamp);
            processApiResponse(response, readableTimestamp);

            currentFetchTimestamp -= (TIME_STEP - TIME_STEP_OVERLAP); // Overlap to ensure no battles are missed

            if (currentFetchTimestamp < OLDEST_HISTORICAL_TIMESTAMP) {
                log.info("Timestamp {} below oldest historical timestamp {}. Switching to forward fetching",
                        currentFetchTimestamp, OLDEST_HISTORICAL_TIMESTAMP);
                switchToFetchingNewReplays();
            }
        } catch (Exception e) {
            log.error("Error fetching historical replays: {}", e.getMessage());
        }
    }

    private List<Battle> fetchBattlesFromApi(long timestamp) {
        String url = UriComponentsBuilder.fromUriString(wavuApiUrl)
                .queryParam("before", timestamp)
                .toUriString();

        ResponseEntity<List<Battle>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful())
            throw new RuntimeException("API request failed with status: " + response.getStatusCode());

        return response.getBody();
    }

    private void switchToFetchingNewReplays() {
        eventPublisherUtils.publishEventForAllGameVersions();

        newestBattleTimestampInDatabase = battleRepository.findNewestRankedBattle()
                .map(Battle::getBattleAt)
                .orElse(Instant.now().getEpochSecond());

        currentFetchTimestamp = Instant.now().getEpochSecond();
        log.info("Database preload complete! Fetching forward starting at: {}", currentFetchTimestamp);
        isFetchingNewReplays = true;
    }

    private void processApiResponse(List<Battle> battles, String readableTimestamp) {
        if (battles == null || battles.isEmpty()) return;

        log.debug("Received response from Wavu API");
        long startTime = System.currentTimeMillis();
        sendToRabbitMQ(battles, readableTimestamp + " UTC");
        log.debug("Sending data to RabbitMQ took {} ms", (System.currentTimeMillis() - startTime));
    }

    void sendToRabbitMQ(List<Battle> battles, String dateAndTime) {
        rabbitTemplate.convertAndSend(
                rabbitMQConfig.getExchangeName(),
                rabbitMQConfig.getRoutingKey(),
                battles,
                msg -> {
                    msg.getMessageProperties()
                            .setHeader(TIMESTAMP_HEADER, dateAndTime);
                    return msg;
                }
        );
    }

    private void resetFetchStateForNewReplays() {
        currentFetchIsBelowNewestBattleInDatabase = false;
        scheduleNextExecution(NEW_REPLAYS_DELAY_MILLIS);
        currentFetchTimestamp = Instant.now().getEpochSecond() + NEW_REPLAYS_DELAY_SECONDS;
        newestBattleTimestampInDatabase = battleRepository.findNewestRankedBattle()
                .map(Battle::getBattleAt)
                .orElse(Instant.now().getEpochSecond());
    }

    private boolean isDatabaseFullyPreloaded(Optional<Battle> oldestBattle) {
        return oldestBattle.isPresent() &&
                oldestBattle.get().getBattleAt() <= OLDEST_HISTORICAL_TIMESTAMP;
    }

    private void initializeForPreloadedDatabase(Battle newestBattle) {
        isFetchingNewReplays = true;
        log.info("Database is preloaded. Fetching new battles.");
        newestBattleTimestampInDatabase = newestBattle.getBattleAt();
        currentFetchTimestamp = Instant.now().getEpochSecond();
    }

    private void initializeForPartiallyLoadedDatabase(Battle oldestBattle) {
        currentFetchTimestamp = oldestBattle.getBattleAt();
        log.info("Continuing historical data fetching, starting at: {}", currentFetchTimestamp);
    }

    private void initializeForEmptyDatabase() {
        tekkenStatsSummaryRepository.initializeStatsSummaryTable();
        currentFetchTimestamp = Instant.now().getEpochSecond();
        log.info("No battles found in database, using current timestamp: {}", currentFetchTimestamp);
    }

    private void scheduleNextExecution(long delayMillis) {
        Instant executionTime = Instant.now().plusMillis(delayMillis);
        scheduledTask = taskScheduler.schedule(this::fetchReplays, executionTime);
    }

    private void checkIfActiveProfileIsDev(String activeProfile) {
        if ("dev".equals(activeProfile) || activeProfile == null || activeProfile.isEmpty()) {
            // Calculate date two weeks from today
            ZonedDateTime twoWeeksFromNow = ZonedDateTime.now(ZoneId.systemDefault()).minusWeeks(2);
            OLDEST_HISTORICAL_TIMESTAMP = twoWeeksFromNow.toEpochSecond();
            log.info("Dev profile detected or no profile set. Setting oldest historical battle timestamp to two weeks from now: {} ({})",
                    OLDEST_HISTORICAL_TIMESTAMP, DateTimeUtils.toReadableTime(OLDEST_HISTORICAL_TIMESTAMP));
        }
    }
}