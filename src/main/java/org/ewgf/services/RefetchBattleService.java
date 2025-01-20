package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;
import org.ewgf.configuration.BackpressureManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Slf4j
public class RefetchBattleService {
    private final RestTemplate restTemplate;
    private final WavuService wavuService;
    private final BackpressureManager backpressureManager;
    private final String wavuApi;
    private static final long STEP_SECONDS = 600;
    private static final long MAX_DAYS = 365;
    private static final long DELAY_BETWEEN_REQUESTS = 500L;

    public RefetchBattleService(
            RestTemplate restTemplate,
            WavuService wavuService,
            BackpressureManager backpressureManager,
            @Value("${wavu.api}") String wavuApi) {
        this.restTemplate = restTemplate;
        this.wavuService = wavuService;
        this.backpressureManager = backpressureManager;
        this.wavuApi = wavuApi;
    }

    public void fetchHistoricalBattles(int daysToFetch) throws IllegalArgumentException, InterruptedException {
        validateInput(daysToFetch);

        long now = Instant.now().getEpochSecond();
        long daysInSeconds = daysToFetch * 24 * 3600L;
        long startTime = now - daysInSeconds;

        backpressureManager.manualBackpressureActivation();
        try {
            processBattlesFetch(now, startTime, daysToFetch);
        } finally {
            backpressureManager.manualBackpressureDeactivation();
        }
    }

    private void validateInput(int daysToFetch) {
        if (daysToFetch <= 0 || daysToFetch > MAX_DAYS) {
            throw new IllegalArgumentException(
                    String.format("Days to fetch must be between 1 and %d", MAX_DAYS));
        }
    }

    private void processBattlesFetch(long endTime, long startTime, int daysToFetch) throws InterruptedException {
        long currentBefore = endTime;
        int processedSteps = 0;
        int totalSteps = calculateTotalSteps(endTime - startTime);

        log.info("Starting historical battle fetch from {} to {} ({} days) in {}-second steps",
                Instant.ofEpochSecond(startTime),
                Instant.ofEpochSecond(endTime),
                daysToFetch,
                STEP_SECONDS);

        while (currentBefore > startTime) {
            fetchAndProcessBattleBatch(currentBefore, ++processedSteps, totalSteps);
            currentBefore -= STEP_SECONDS;
            Thread.sleep(DELAY_BETWEEN_REQUESTS);
        }

        log.info("Completed historical battle fetch for {} days of data.", daysToFetch);
    }

    private void fetchAndProcessBattleBatch(long currentBefore, int currentStep, int totalSteps) {
        String url = buildUrl(currentBefore);
        log.info("Requesting replays: battle_at <= {} AND battle_at > {} (Progress: {}/{})",
                currentBefore, currentBefore - 700, currentStep, totalSteps);

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            processApiResponse(jsonResponse, currentBefore);
        } catch (Exception e) {
            log.error("Error fetching replays for 'before={}', skipping step. Reason: {}",
                    currentBefore, e.getMessage());
        }
    }

    private String buildUrl(long before) {
        return wavuApi + "?before=" + before;
    }

    private int calculateTotalSteps(long totalSeconds) {
        return (int) (totalSeconds / STEP_SECONDS);
    }

    private void processApiResponse(String jsonResponse, long beforeValue) {
        if (jsonResponse == null) {
            log.warn("Got empty response from Wavu API for 'before={}'", beforeValue);
            return;
        }

        log.debug("Received response of length {} for 'before={}'", jsonResponse.length(), beforeValue);
        wavuService.sendToRabbitMQ(jsonResponse, String.valueOf(beforeValue));
    }
}
