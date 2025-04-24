package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ewgf.configuration.BackpressureManager;
import org.ewgf.configuration.MessageConsumptionManager;
import org.ewgf.configuration.RabbitMQConfig;
import org.ewgf.services.CharacterStatsRevalidationService;
import org.ewgf.services.RefetchBattleService;
import org.ewgf.utils.EventPublisherUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {
    private final CharacterStatsRevalidationService revalidationService;
    private final String devAuthToken;
    private final RefetchBattleService refetchBattleService;
    private final EventPublisherUtils eventPublisherUtils;
    private final MessageConsumptionManager messageConsumptionManager;

    public AdminController(
            CharacterStatsRevalidationService revalidationService,
            @Value("${admin.auth.token}") String devAuthToken,
            RefetchBattleService refetchBattleService,
            EventPublisherUtils eventPublisherUtils,
            MessageConsumptionManager messageConsumptionManager) {

        this.revalidationService = revalidationService;
        this.devAuthToken = devAuthToken;
        this.refetchBattleService = refetchBattleService;
        this.eventPublisherUtils = eventPublisherUtils;
        this.messageConsumptionManager = messageConsumptionManager;
    }

    private boolean isAuthenticated(String authToken) {
        if (authToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                devAuthToken.getBytes(StandardCharsets.UTF_8),
                authToken.getBytes(StandardCharsets.UTF_8)
        );    }


    @GetMapping("/revalidateCharacterStats")
    public ResponseEntity<String> revalidateCharacterStats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authToken,
            HttpServletRequest request) {

        log.info("Received revalidation request from IP: {}", request.getRemoteAddr());

        if (!isAuthenticated(authToken)) {
            log.warn("Unauthorized revalidation attempt from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        // Start the revalidation process in a separate thread
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting asynchronous revalidation process");
                revalidationService.startRevalidation();
                log.info("Revalidation process completed successfully");
            } catch (Exception e) {
                log.error("Error during asynchronous revalidation", e);
            }
        });

        // Return immediately without waiting for the process to complete
        return ResponseEntity.accepted().body("Revalidation process started successfully");
    }


    @GetMapping("/refetch")
    public ResponseEntity<String> fetchHistoricalBattles(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authToken,
            @RequestHeader(value = "X-Days-To-Fetch", defaultValue = "5") Integer daysToFetch,
            HttpServletRequest request) {

        if (!isAuthenticated(authToken)) {
            log.warn("Unauthorized refetch attempt from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            refetchBattleService.fetchHistoricalBattles(daysToFetch);
            return ResponseEntity.ok(String.format("Manual fetch of last %d days completed successfully.", daysToFetch));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error during historical battle fetch", e);
            return ResponseEntity.internalServerError()
                    .body("Error during historical battle fetch: ");
        }
    }

    @GetMapping("/recalculateStats")
    public ResponseEntity<String> recalculateStats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authToken,
            HttpServletRequest request)
    {

        if(!isAuthenticated(authToken)) {
            log.warn("Unauthorized recalc attempt from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        try{
            eventPublisherUtils.publishEventForAllGameVersions();
        }
        catch (Exception e) {
            log.error("Error recalculating stats", e);
            return ResponseEntity.internalServerError().body("Error recalculating stats");
        }
        return ResponseEntity.ok("Successfully Recalculated stats");
    }

    @GetMapping("/pause")
    public ResponseEntity<String> pauseRabbitMQConsumption(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
            HttpServletRequest request) {

        if (!isAuthenticated(authToken)) {
            log.warn("Unauthorized pause request from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        messageConsumptionManager.pauseAllConsumers();
        log.info("RabbitMQ consumption paused by {}", request.getRemoteAddr());
        return ResponseEntity.ok("RabbitMQ consumers paused");
    }

    @GetMapping("/resume")
    public ResponseEntity<String> resumeRabbitMQConsumption(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
            HttpServletRequest request) {

        if (!isAuthenticated(authToken)) {
            log.warn("Unauthorized resume request from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        messageConsumptionManager.resumeAllConsumers();
        log.info("RabbitMQ consumption resumed by {}", request.getRemoteAddr());
        return ResponseEntity.ok("RabbitMQ consumers resumed");
    }
}