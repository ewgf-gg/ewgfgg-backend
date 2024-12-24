package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;

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

@Service
@Slf4j
public class WavuService implements InitializingBean, DisposableBean {

    private final RabbitTemplate rabbitTemplate;
    private final BackpressureManager backpressureManager;
    private final RestTemplate restTemplate;
    private final BattleRepository battleRepository;
    private final TaskScheduler taskScheduler;
    private final TekkenStatsSummaryRepository tekkenStatsSummaryRepository;
    private final StatisticsEventPublisher eventPublisher;
    private final CharacterStatsRepository characterStatsRepository;
    private final int TIME_STEP = 700;
    private final ZoneId zoneId = ZoneId.of("UTC");
    private boolean isFetchingForward = false;
    private long currentFetchTimestamp;
    private long lastFetchedSystemTimestamp;
    private final long OLDEST_HISTORICAL_TIMESTAMP = 1711548580L;
    private long newestKnownBattleTimestamp;
    private ScheduledFuture<?> scheduledTask;
    private boolean fetchIsAheadOfCurrent = false;

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
            CharacterStatsRepository characterStatsRepository
    )
    {
        this.rabbitTemplate = rabbitTemplate;
        this.backpressureManager = backpressureManager;
        this.restTemplate = restTemplate;
        this.battleRepository = battleRepository;
        this.taskScheduler = taskScheduler;
        this.tekkenStatsSummaryRepository = tekkenStatsSummaryRepository;
        this.eventPublisher = eventPublisher;
        this.characterStatsRepository = characterStatsRepository;
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

            if (oldestBattle.isPresent() && oldestBattle.get().getBattleAt() == 1711548580)
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
            currentFetchTimestamp = getPresentUnixTimestamp();
        }
        lastFetchedSystemTimestamp = getPresentUnixTimestamp();
    }

    private void initializeForPartiallyLoadedDatabase(Battle oldestBattle)
    {
        currentFetchTimestamp = oldestBattle.getBattleAt();
        log.info("Continuing historical data fetching, starting at: {}", currentFetchTimestamp);
    }

    private void initializeForEmptyDatabase()
    {
        tekkenStatsSummaryRepository.initializeStatsSummaryTable();
        currentFetchTimestamp = getPresentUnixTimestamp();
        log.info("No battles found in database, using current timestamp: {}", currentFetchTimestamp);
    }

    private void scheduleNextExecution(long delayMillis)
    {
        Instant executionTime = Instant.now().plusMillis(delayMillis);
        scheduledTask = taskScheduler.schedule(this::fetchReplays, executionTime);
    }

    private void fetchReplays()
    {
        if (backpressureManager.isBackpressureActive())
        {
            log.warn("BACKPRESSURE ACTIVE: MESSAGE RETRIEVAL IS STOPPED");
            scheduleNextExecution(1200L * backpressureManager.getSlowdownFactor());
            return;
        }

        if (isFetchingForward)
        {
            fetchForward();

            if(fetchIsAheadOfCurrent)
            {
                fetchIsAheadOfCurrent = false;
                scheduleNextExecution((TIME_STEP-620) * 1000);
                return;
            }
        }
        else
        {
            fetchBackward();
        }

        scheduleNextExecution(300);
    }

    private void fetchForward()
    {
        if (currentFetchTimestamp > lastFetchedSystemTimestamp)
        {
            log.info("Current fetch timestamp {} {} is greater than last system's " +
                    "timestamp {} {}",
                    currentFetchTimestamp,ReadableTimeFromUnixTimestamp(currentFetchTimestamp),
                    lastFetchedSystemTimestamp, ReadableTimeFromUnixTimestamp(lastFetchedSystemTimestamp));

            fetchIsAheadOfCurrent = true;
            currentFetchTimestamp = lastFetchedSystemTimestamp;
            lastFetchedSystemTimestamp = getPresentUnixTimestamp();
        }

        String dateFromUnix = ReadableTimeFromUnixTimestamp(currentFetchTimestamp);
        log.info("Sending query to API endpoint for battle time after: {} UTC, Unix: {}", dateFromUnix, currentFetchTimestamp);

        try
        {
            String jsonResponse = restTemplate.getForObject(WAVU_API + "?before=" + currentFetchTimestamp, String.class);
            processApiResponse(jsonResponse, dateFromUnix);
            currentFetchTimestamp += TIME_STEP-100;
        }
        catch (Exception e)
        {
            log.error("Error fetching forward or sending data: {}", e.getMessage());
        }
    }

    private void fetchBackward()
    {
        try
        {
            String dateFromUnix = ReadableTimeFromUnixTimestamp(currentFetchTimestamp);
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
                switchToForwardFetching();
            }
        } catch (Exception e)
        {
            log.error("Error while fetching backward: {}", e.getMessage());
        }
    }

    private void switchToForwardFetching()
    {
        log.info("Switching to forward fetching, publishing event for statistics calculation.");

        Optional<List<Integer>> gameVersions = characterStatsRepository.findAllGameVersions();
        gameVersions.ifPresent(integers -> eventPublisher.tryPublishEvent(
                new ReplayProcessingCompletedEvent(new HashSet<>(integers))));

        Optional<Battle> newestBattle = battleRepository.findNewestBattle();

        if (newestBattle.isPresent())
        {
            newestKnownBattleTimestamp = newestBattle.get().getBattleAt();
            currentFetchTimestamp = newestKnownBattleTimestamp + TIME_STEP;
        }
        else
        {
            currentFetchTimestamp = getPresentUnixTimestamp();
        }
        lastFetchedSystemTimestamp = getPresentUnixTimestamp();
        log.info("Database preload complete! Fetching forward starting at: {}", currentFetchTimestamp);
        isFetchingForward = true;
    }

    private void processApiResponse(String jsonResponse, String dateFromUnix)
    {
        if (jsonResponse != null)
        {
            log.info("Received response from Wavu Api");
            long startTime = System.currentTimeMillis();
            sendToRabbitMQ(jsonResponse, dateFromUnix + " UTC");
            long endTime = System.currentTimeMillis();
            log.info("Sending data to RabbitMQ took {} ms", (endTime - startTime));
        }
    }

    public void sendToRabbitMQ(String message, String dateAndTime)
    {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties()
                            .setHeader("unixTimestamp", dateAndTime);
                    return msg;
                }
        );
    }

    private String ReadableTimeFromUnixTimestamp(long unixTimestamp)
    {
        return Instant.ofEpochSecond(unixTimestamp)
                .atZone(zoneId)
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
    }

    private long getPresentUnixTimestamp()
    {
        return Instant.now().getEpochSecond();
    }
}
