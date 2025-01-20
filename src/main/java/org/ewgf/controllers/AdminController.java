package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ewgf.configuration.BackpressureManager;
import org.ewgf.services.CharacterStatsRevalidationService;
import org.ewgf.services.RefetchBattleService;
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
@RequestMapping("/admin")
public class AdminController {
    private final CharacterStatsRevalidationService revalidationService;
    private final String devAuthToken;
    private final RefetchBattleService refetchBattleService;

    public AdminController(
            CharacterStatsRevalidationService revalidationService,
            @Value("${admin.auth.token}") String devAuthToken, RefetchBattleService refetchBattleService) {

        this.revalidationService = revalidationService;
        this.devAuthToken = devAuthToken;
        this.refetchBattleService = refetchBattleService;
    }

    private boolean isAuthenticated(String authToken) {
        return devAuthToken != null && devAuthToken.equals(authToken);
    }


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
            return ResponseEntity.ok("Revalidation process finished successfully");
        } catch (Exception e) {
            log.error("Error starting revalidation", e);
            return ResponseEntity.internalServerError().body("Error starting revalidation: " + e.getMessage());
        }
    }


    @GetMapping("/recalc")
    public ResponseEntity<String> fetchHistoricalBattles(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authToken,
            @RequestHeader(value = "X-Days-To-Fetch", defaultValue = "5") Integer daysToFetch,
            HttpServletRequest request) {

        if (isAuthenticated(authToken)) {
            log.warn("Unauthorized recalc attempt from IP: {}", request.getRemoteAddr());
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
                    .body("Error during historical battle fetch: " + e.getMessage());
        }
    }
}