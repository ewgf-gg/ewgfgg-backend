package org.tekkenstats.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tekkenstats.configuration.BackpressureManager;
import org.tekkenstats.configuration.RabbitMQConfig;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class APIService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    BackpressureManager backpressureManager;
    @Autowired
    private RestTemplate restTemplate;

    private static final Logger logger = LogManager.getLogger(APIService.class);
    private final int TIME_STEP = 700;

    private String API_URL = "https://wank.wavu.wiki/api/replays";
    private static final ZoneId zoneId = ZoneId.of("UTC");

    long unixTimestamp = 1728495479L;

    @Scheduled(fixedRate = 1200)
    public void fetchReplays()
    {
        if (Backpressure())
        {
            return;
        }

        String dateFromUnix = formatUnixTimestamp();
        logger.info("Sending query to API endpoint for battle time before: {} UTC, Unix: {}", dateFromUnix, unixTimestamp);

        try
        {
                String jsonResponse = restTemplate.getForObject(API_URL + "?before=" + unixTimestamp, String.class);
                processApiResponse(jsonResponse, dateFromUnix);
        }
        catch (Exception e)
        {
            logger.error("Error fetching or sending data: {}", e.getMessage());
        }
    }

    private boolean Backpressure()
    {
        if (backpressureManager.isBackpressureActive())
        {
            logger.warn("BACKPRESSURE ACTIVE: MESSAGE RETRIEVAL IS STOPPED");
            try
            {
                Thread.sleep(1200L * backpressureManager.getSlowdownFactor());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            return true;
        }
        return false;
    }

    private String formatUnixTimestamp()
    {
        return Instant.ofEpochSecond(unixTimestamp)
                .atZone(zoneId)
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
    }

    private void processApiResponse(String jsonResponse, String dateFromUnix)
    {
        if (jsonResponse != null) {
            logger.info("Received response from API");
            long startTime = System.currentTimeMillis();
            sendToRabbitMQ(jsonResponse, dateFromUnix + " UTC");
            long endTime = System.currentTimeMillis();
            logger.info("Sending data to RabbitMQ took {} ms", (endTime - startTime));

            unixTimestamp -= TIME_STEP;
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
}

