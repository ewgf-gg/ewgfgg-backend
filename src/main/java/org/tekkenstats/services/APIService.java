package org.tekkenstats.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.tekkenstats.configuration.BackpressureManager;
import org.tekkenstats.configuration.RabbitMQConfig;
import org.tekkenstats.models.Battle;
import org.tekkenstats.repositories.BattleRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Service
public class APIService implements InitializingBean, DisposableBean {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private BackpressureManager backpressureManager;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private BattleRepository battleRepository;
    @Autowired
    private TaskScheduler taskScheduler;

    private static final Logger logger = LogManager.getLogger(APIService.class);
    private static final int TIME_STEP = 700;
    private static final ZoneId zoneId = ZoneId.of("UTC");

    @Value("${API_URL}")
    private String API_URL;

    private boolean isFetchingForward = false;
    private long currentFetchTimestamp; // renamed from unixTimestamp
    private long lastFetchedSystemTimestamp; // renamed from currentTimestamp
    private final long oldestHistoricalBattleTimestamp = 1711548580L ; // renamed from oldestBattleTimestamp
    private long newestKnownBattleTimestamp; // renamed from newestBattleTimestamp
    private ScheduledFuture<?> scheduledTask;
    private boolean fetchIsAheadOfCurrent = false;

    @Override
    public void afterPropertiesSet() {
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
            logger.error("Error initializing unixTimestamp: {}", e.getMessage());
            System.exit(-1);
        }
    }

    private void initializeForPreloadedDatabase(Optional<Battle> newestBattle)
    {
        isFetchingForward = true;
        logger.info("Database is preloaded. Fetching new battles.");
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
        logger.info("Continuing historical data fetching, starting at: {}", currentFetchTimestamp);
    }

    private void initializeForEmptyDatabase()
    {
        currentFetchTimestamp = getPresentUnixTimestamp();
        logger.info("No battles found in database, using current timestamp: {}", currentFetchTimestamp);
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
            logger.warn("BACKPRESSURE ACTIVE: MESSAGE RETRIEVAL IS STOPPED");
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
            logger.info("Current fetch timestamp {} {} is greater than last system's " +
                    "timestamp {} {}",
                    currentFetchTimestamp,ReadableTimeFromUnixTimestamp(currentFetchTimestamp),
                    lastFetchedSystemTimestamp, ReadableTimeFromUnixTimestamp(lastFetchedSystemTimestamp));
            fetchIsAheadOfCurrent = true;
            currentFetchTimestamp = lastFetchedSystemTimestamp;
            lastFetchedSystemTimestamp = getPresentUnixTimestamp();
        }

        String dateFromUnix = ReadableTimeFromUnixTimestamp(currentFetchTimestamp);
        logger.info("Sending query to API endpoint for battle time after: {} UTC, Unix: {}", dateFromUnix, currentFetchTimestamp);

        try
        {
            String jsonResponse = restTemplate.getForObject(API_URL + "?after=" + currentFetchTimestamp, String.class);
            processApiResponse(jsonResponse, dateFromUnix);
            currentFetchTimestamp += TIME_STEP;
        }
        catch (Exception e)
        {
            logger.error("Error fetching forward or sending data: {}", e.getMessage());
        }
    }

    private void fetchBackward() {
        String dateFromUnix = ReadableTimeFromUnixTimestamp(currentFetchTimestamp);
        logger.info("Sending query to API endpoint for battle time before: {} UTC, Unix: {}", dateFromUnix, currentFetchTimestamp);

        try {
            String jsonResponse = restTemplate.getForObject(API_URL + "?before=" + currentFetchTimestamp, String.class);
            processApiResponse(jsonResponse, dateFromUnix);
            currentFetchTimestamp -= TIME_STEP;

            if (currentFetchTimestamp < oldestHistoricalBattleTimestamp)
            {
                logger.info("Current timestamp of {} is now below the oldest historical timestamp {}. " +
                        "Switching to forward fetching", currentFetchTimestamp, oldestHistoricalBattleTimestamp);
                switchToForwardFetching();
            }
        } catch (Exception e)
        {
            logger.error("Error fetching backward or sending data: {}", e.getMessage());
        }
    }

    private void switchToForwardFetching()
    {
        isFetchingForward = true;
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
        logger.info("Switching to forward fetching. Starting from: {}", currentFetchTimestamp);
    }


    private void processApiResponse(String jsonResponse, String dateFromUnix)
    {
        if (jsonResponse != null)
        {
            logger.info("Received response from API");
            long startTime = System.currentTimeMillis();
            sendToRabbitMQ(jsonResponse, dateFromUnix + " UTC");
            long endTime = System.currentTimeMillis();
            logger.info("Sending data to RabbitMQ took {} ms", (endTime - startTime));
        }
    }

    public void sendToRabbitMQ(String message, String dateAndTime)
    {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setHeader("unixTimestamp", dateAndTime);
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
