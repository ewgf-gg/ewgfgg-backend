package org.ewgf.controllers;

import org.ewgf.dtos.*;
import org.ewgf.models.Player;
import org.ewgf.services.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/player-stats")
public class PlayerController {
    private final PlayerService playerService;
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/{player}")
    public ResponseEntity<PlayerDTO> getPlayerStats(@PathVariable String player) {
        logger.info("Received request for Player: {}", player);
        PlayerDTO playerDTO = playerService.getPlayerStats(player);
        if (playerDTO == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(playerDTO);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchDTO>> searchPlayers(@RequestParam String query) {
        if (query.isEmpty() || query.length() >= 20) return ResponseEntity.badRequest().build();
        List<PlayerSearchDTO> projections = playerService.searchPlayers(query);
        return ResponseEntity.ok(projections);
    }

    @GetMapping("/metaData/{polarisId}")
    public ResponseEntity<PlayerMetadataDTO> getPlayerMetadata(@PathVariable String polarisId) {
        PlayerMetadataDTO metadata = playerService.getPlayerMetadata(polarisId);
        if (metadata == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/recentlyActive")
    public ResponseEntity<List<RecentlyActivePlayersDTO>> getRecentlyActivePlayers() {
        List<RecentlyActivePlayersDTO> recentlyActivePlayers = playerService.getRecentlyActivePlayers();
        if (recentlyActivePlayers == null || recentlyActivePlayers.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(recentlyActivePlayers);
    }
}