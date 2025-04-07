package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.ewgf.dtos.*;
import org.ewgf.dtos.header_or_footer.PlayerSearchDTO;
import org.ewgf.dtos.homepage.RegionalPlayerDistributionDTO;
import org.ewgf.dtos.player_stats_page.PlayerDTO;
import org.ewgf.dtos.player_stats_page.PlayerMetadataDTO;
import org.ewgf.services.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.ewgf.utils.Constants.MAX_QUERY_LENGTH;

@RestController
@RequestMapping("/player-stats")
public class PlayerController {
    private final PlayerService playerService;
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/{polarisId}")
    public ResponseEntity<PlayerDTO> getPlayerStats(@PathVariable String polarisId, HttpServletRequest request) {
        logger.info("Received request for Player: {} from IP: {}", polarisId, request.getRemoteAddr());
        PlayerDTO playerDTO = playerService.getPlayerStats(polarisId);
        if (playerDTO == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(playerDTO);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchDTO>> searchPlayers(@RequestParam String query) {
        if (query == null || query.isBlank() || query.length() >= MAX_QUERY_LENGTH) return ResponseEntity.badRequest().build();
        List<PlayerSearchDTO> projections = playerService.searchPlayers(query);
        return ResponseEntity.ok(projections);
    }

    @GetMapping("/metaData/{polarisId}")
    public ResponseEntity<PlayerMetadataDTO> getPlayerMetadata(@PathVariable String polarisId) {
        PlayerMetadataDTO metadata = playerService.getPlayerMetadata(polarisId);
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/recentlyActive")
    public ResponseEntity<List<RecentlyActivePlayersDTO>> getRecentlyActivePlayers() {
        List<RecentlyActivePlayersDTO> recentlyActivePlayers = playerService.getRecentlyActivePlayers();
        return ResponseEntity.ok(recentlyActivePlayers);
    }

    @GetMapping("/regionalPlayerDistribution")
    public ResponseEntity<RegionalPlayerDistributionDTO> getRegionalPlayerDistribution() {
        RegionalPlayerDistributionDTO distribution = playerService.getRegionalPlayerDistribution();
        return ResponseEntity.ok(distribution);
    }
}