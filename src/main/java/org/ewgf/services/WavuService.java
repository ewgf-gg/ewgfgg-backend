package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;

import org.ewgf.utils.DateTimeUtils;
import org.ewgf.utils.EventPublisherUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.ewgf.configuration.BackpressureManager;
import org.ewgf.configuration.RabbitMQConfig;
import org.ewgf.events.ReplayProcessingCompletedEvent;
import org.ewgf.events.StatisticsEventPublisher;
import org.ewgf.models.Battle;
import org.ewgf.repositories.BattleRepository;
import org.ewgf.repositories.CharacterStatsRepository;
import org.ewgf.repositories.TekkenStatsSummaryRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class WavuService implements InitializingBean, DisposableBean {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;
    private final BackpressureManager backpressureManager;
    private final RestTemplate restTemplate;
    private final BattleRepository battleRepository;
    private final TaskScheduler taskScheduler;
    private final TekkenStatsSummaryRepository tekkenStatsSummaryRepository;
    private final CharacterStatsRepository characterStatsRepository;
    private final ZoneId zoneId = ZoneId.of("UTC");
    private ScheduledFuture<?> scheduledTask;
    private final EventPublisherUtils eventPublisherUtils;

    private final int TIME_STEP = 700;
    private boolean isFetchingForward = false;
    private long currentFetchTimestamp;
    private long latestBattleTimestamp;
    private final long OLDEST_HISTORICAL_TIMESTAMP = 1711548580L;
    private long newestKnownBattleTimestamp;
    private boolean fetchIsBelowCurrent = false;

    @Value("${wavu.api}")
    private String WAVU_API;
    
    public WavuService(
            RabbitTemplate rabbitTemplate,
            BackpressureManager backpressureManager,
            RestTemplate restTemplate,
            BattleRepository battleRepository,
            TaskScheduler taskScheduler,
            TekkenStatsSummaryRepository tekkenStatsSummaryRepository,
            StatisticsEventPublisher eventPublisher,
            CharacterStatsRepository characterStatsRepository,
            RabbitMQConfig rabbitMQConfig,
            EventPublisherUtils eventPublisherUtils
    )
    {
        this.rabbitTemplate = rabbitTemplate;
        this.backpressureManager = backpressureManager;
        this.restTemplate = restTemplate;
        this.battleRepository = battleRepository;
        this.taskScheduler = taskScheduler;
        this.tekkenStatsSummaryRepository = tekkenStatsSummaryRepository;
        this.characterStatsRepository = characterStatsRepository;
        this.rabbitMQConfig = rabbitMQConfig;
        this.eventPublisherUtils = eventPublisherUtils;
    }
    
    @Override
    public void afterPropertiesSet()
    {
        init();
        scheduleNextExecution(0);
    }

    @Override
    public void destroy()
    {
        if (scheduledTask != null)
        {
            scheduledTask.cancel(true);
        }
    }

    private void init()
    {
        try
        {
            Optional<Battle> oldestBattle = battleRepository.findOldestBattle();
            Optional<Battle> newestBattle = battleRepository.findNewestBattle();

            if (oldestBattle.isPresent() && oldestBattle.get().getBattleAt() == OLDEST_HISTORICAL_TIMESTAMP)
            {
                initializeForPreloadedDatabase(newestBattle);
            }
            else if (oldestBattle.isPresent())
            {
                initializeForPartiallyLoadedDatabase(oldestBattle.get());
            }
            else
            {
                initializeForEmptyDatabase();
            }
        }
        catch (Exception e)
        {
            log.error("Error initializing server: {}", e.getMessage());
            System.exit(-1);
        }
    }
    
    
    private void fetchReplays()
    {
        if (backpressureManager.isBackpressureActive())
        {
            if(backpressureManager.isManuallyActivated())
            {
                log.warn("MESSAGE RETRIEVAL HAS BEEN MANUALLY PAUSED.");
                scheduleNextExecution(60000); // 1 minute
                return;
            }
            log.warn("BACKPRESSURE ACTIVE: MESSAGE RETRIEVAL IS STOPPED");
            scheduleNextExecution(1200L * backpressureManager.getSlowdownFactor());
            return;
        }

        if (isFetchingForward)
        {
            fetchNewReplays();

            if(fetchIsBelowCurrent)
            {
                fetchIsBelowCurrent = false;
                scheduleNextExecution((TIME_STEP-660) * 1000); // fetch again in one minute
                currentFetchTimestamp = Instant.now().getEpochSecond() + 30; //set current fetch to the futture schedule time
                latestBattleTimestamp = battleRepository.findNewestBattle().get().getBattleAt();
                return;
            }
        }
        else
        {
            fetchHistoricalReplays();
        }

        scheduleNextExecution(300);
    }

    private void fetchNewReplays()
    {
        if (currentFetchTimestamp < latestBattleTimestamp)
        {

            log.info("Current fetch timestamp {} {} is below than latest database " + "timestamp {} {}",
                    currentFetchTimestamp,
                    DateTimeUtils.toReadableTime(currentFetchTimestamp),
                    latestBattleTimestamp,
                    DateTimeUtils.toReadableTime(latestBattleTimestamp));
            
            currentFetchTimestamp = (latestBattleTimestamp + (TIME_STEP-1));
            fetchIsBelowCurrent = true;
        }

        String dateFromUnix = DateTimeUtils.toReadableTime(currentFetchTimestamp);
        log.info("Fetching battles before timestamp: {} UTC, Unix: {}", dateFromUnix, currentFetchTimestamp);

        try
        {
            String jsonResponse = restTemplate.getForObject(WAVU_API + "?before=" + currentFetchTimestamp, String.class);
            processApiResponse(jsonResponse, dateFromUnix);
            currentFetchTimestamp -= (TIME_STEP-100);
        }
        catch (Exception e)
        {
            log.error("Error fetching forward or sending data: {}", e.getMessage());
        }
    }

    private void fetchHistoricalReplays()
    {
        try
        {
            String dateFromUnix = DateTimeUtils.toReadableTime(currentFetchTimestamp);
            log.info("Fetching battles before: {} UTC (Unix: {})", dateFromUnix, currentFetchTimestamp);

            String url = UriComponentsBuilder.fromUriString(WAVU_API)
                    .queryParam("before", currentFetchTimestamp)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API request failed with status: " + response.getStatusCode());
            }

            // Process response and update timestamp
            processApiResponse(response.getBody(), dateFromUnix);
            currentFetchTimestamp -= (TIME_STEP - 60); // -60 to overlap times

            if (currentFetchTimestamp < OLDEST_HISTORICAL_TIMESTAMP)
            {
                log.info("Timestamp {} below oldest historical timestamp {}. Switching to forward fetching",
                        currentFetchTimestamp, OLDEST_HISTORICAL_TIMESTAMP);
                switchToFetchingNewReplays();
            }
        }
        catch (Exception e)
        {
            log.error("Error while fetching backward: {}", e.getMessage());
        }
    }

    private void switchToFetchingNewReplays()
    {
        log.info("Switching to forward fetching, publishing event for statistics calculation.");

        eventPublisherUtils.publishEventForAllGameVersions();

        Optional<Battle> newestBattle = battleRepository.findNewestBattle();

        if (newestBattle.isPresent())
        {
            latestBattleTimestamp = newestBattle.get().getBattleAt();
            currentFetchTimestamp = Instant.now().getEpochSecond();
        }
        else
        {
            currentFetchTimestamp = Instant.now().getEpochSecond();
        }
        latestBattleTimestamp = Instant.now().getEpochSecond();
        log.info("Database preload complete! Fetching forward starting at: {}", currentFetchTimestamp);
        isFetchingForward = true;
    }

    private void processApiResponse(String jsonResponse, String dateFromUnix)
    {
        if (jsonResponse != null)
        {
            log.debug("Received response from Wavu Api");
            long startTime = System.currentTimeMillis();
            sendToRabbitMQ(jsonResponse, dateFromUnix + " UTC");
            log.debug("Sending data to RabbitMQ took {} ms", (System.currentTimeMillis() - startTime));
        }
    }

    public void sendToRabbitMQ(String message, String dateAndTime)
    {
        rabbitTemplate.convertAndSend(
                rabbitMQConfig.getExchangeName(),
                rabbitMQConfig.getRoutingKey(),
                message,
                msg -> {
                    msg.getMessageProperties()
                            .setHeader("unixTimestamp", dateAndTime);
                    return msg;
                }
        );
    }

    private void initializeForPreloadedDatabase(Optional<Battle> newestBattle)
    {
        isFetchingForward = true;
        log.info("Database is preloaded. Fetching new battles.");

        if (newestBattle.isPresent())
        {
            newestKnownBattleTimestamp = newestBattle.get().getBattleAt();
            currentFetchTimestamp = newestKnownBattleTimestamp + TIME_STEP;
        }
        else
        {
            currentFetchTimestamp = Instant.now().getEpochSecond();
        }
        latestBattleTimestamp = Instant.now().getEpochSecond();
    }

    private void initializeForPartiallyLoadedDatabase(Battle oldestBattle)
    {
        currentFetchTimestamp = oldestBattle.getBattleAt();
        log.info("Continuing historical data fetching, starting at: {}", currentFetchTimestamp);
    }

    private void initializeForEmptyDatabase()
    {
        tekkenStatsSummaryRepository.initializeStatsSummaryTable();
        currentFetchTimestamp = Instant.now().getEpochSecond();
        log.info("No battles found in database, using current timestamp: {}", currentFetchTimestamp);
    }

    private void scheduleNextExecution(long delayMillis)
    {
        Instant executionTime = Instant.now().plusMillis(delayMillis);
        scheduledTask = taskScheduler.schedule(this::fetchReplays, executionTime);
    }
}
