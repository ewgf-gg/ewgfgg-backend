package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ewgf.configuration.BackpressureManager;
import org.ewgf.services.CharacterStatsRevalidationService;
import org.ewgf.services.WavuService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/dev")
public class AdminController {

    private final RestTemplate restTemplate;
    private final WavuService wavuService;
    private final CharacterStatsRevalidationService revalidationService;
    private final String devAuthToken;
    private final String wavuApi;
    private final BackpressureManager backpressureManager;

    public AdminController(
            RestTemplate restTemplate,
            WavuService wavuService,
            CharacterStatsRevalidationService revalidationService,
            @Value("${wavu.api}") String wavuApi,
            @Value("${admin.auth.token}") String devAuthToken, BackpressureManager backpressureManager) {
        this.restTemplate = restTemplate;
        this.wavuService = wavuService;
        this.revalidationService = revalidationService;
        this.wavuApi = wavuApi;
        this.devAuthToken = devAuthToken;
        this.backpressureManager = backpressureManager;
    }

    /**
     * Authenticates the request using the X-Dev-Token header
     */
    private boolean isAuthenticated(String authToken) {
        return devAuthToken != null && devAuthToken.equals(authToken);
    }

    /**
     * Triggers a revalidation of all character statistics
     */
    @GetMapping("/revalidate")
    public ResponseEntity<String> revalidateStats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authToken,
            HttpServletRequest request) {

        log.info("Received revalidation request from IP: {}", request.getRemoteAddr());

        if (!isAuthenticated(authToken)) {
            log.warn("Unauthorized revalidation attempt from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            revalidationService.startRevalidation();
            return ResponseEntity.ok("Revalidation process started successfully");
        } catch (Exception e) {
            log.error("Error starting revalidation", e);
            return ResponseEntity.internalServerError().body("Error starting revalidation: " + e.getMessage());
        }
    }

    /**
     * DEV-ONLY ENDPOINT
     * This will fetch ~5 hours of battles in 10-minute steps
     * by calling /api/replays?before=xxx for each step.
     */
    @GetMapping("/recalc")
    public ResponseEntity<String> fetchLast5hoursOfBattles(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authToken,
            HttpServletRequest request) throws InterruptedException {

        if (!isAuthenticated(authToken)) {
            log.warn("Unauthorized recalc attempt from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        long now = Instant.now().getEpochSecond();
        long fiveHoursAgo = now - (5 * 3600); // 5 hours * 3600 seconds/hour = 18000
        long stepSeconds = 600;

        backpressureManager.manualBackpressureActivation();

        log.info("Manually fetching battles from {} to {} (5 hours) in {}-second steps",
                Instant.ofEpochSecond(fiveHoursAgo),
                Instant.ofEpochSecond(now),
                stepSeconds);

        long currentBefore = now;

        while (currentBefore > fiveHoursAgo) {
            String url = wavuApi + "?before=" + currentBefore;
            log.info("Requesting replays: battle_at <= {} AND battle_at > {}",
                    currentBefore, currentBefore - 700);

            try {
                String jsonResponse = restTemplate.getForObject(url, String.class);
                processApiResponse(jsonResponse, currentBefore);
            } catch (Exception e) {
                log.error("Error fetching replays for 'before={}', skipping step. Reason: {}",
                        currentBefore, e.getMessage());
            }

            currentBefore -= stepSeconds;
            Thread.sleep(500L);
        }

        backpressureManager.manualBackpressureDeactivation();
        log.info("Finished Manual fetch.");
        return ResponseEntity.ok("Manual fetch of last 5 hours completed successfully.");
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