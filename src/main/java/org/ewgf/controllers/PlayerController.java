package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.ewgf.dtos.*;
import org.ewgf.models.Player;
import org.ewgf.response.StatPentagonResponse;
import org.ewgf.services.PlayerService;
import org.ewgf.services.PolarisProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.ewgf.utils.Constants.GET_PROFILE;
import static org.ewgf.utils.Constants.USER_ID;

@RestController
@RequestMapping("/player-stats")
public class PlayerController {
    private final PlayerService playerService;
    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);
    private final PolarisProxyService polarisProxyService;

    public PlayerController(PlayerService playerService, PolarisProxyService polarisProxyService) {
        this.playerService = playerService;
        this.polarisProxyService = polarisProxyService;
    }

    @GetMapping("/{polarisId}")
    public ResponseEntity<PlayerDTO> getPlayerStats(@PathVariable String polarisId, HttpServletRequest request) throws Exception {
        long requestStartTime = System.currentTimeMillis();
        PlayerDTO playerDTO = playerService.getPlayerStats(polarisId);
        if (playerDTO == null) return ResponseEntity.notFound().build();
        log.info("Requested player {} completed in {} ms", polarisId, System.currentTimeMillis() - requestStartTime);
        return ResponseEntity.ok(playerDTO);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchDTO>> searchPlayers(@RequestParam String query) {
        if (query == null || query.trim().isBlank() || query.trim().length() >= 20) {
            log.warn("Invalid search query: {}", query);
            return ResponseEntity.badRequest().build();
        }
        log.info("Received search query: {}", query);
        List<PlayerSearchDTO> projections = playerService.searchPlayers(query.trim());
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

    @GetMapping("/getStatPentagon")
    public ResponseEntity<StatPentagonResponse> getPolarisIdMapping(@RequestParam String polarisId) throws Exception {
        String playerId = playerService.getPlayerIdFromPolarisId(polarisId);

        if (playerId == null || playerId.isEmpty()) {
            log.warn("No player found for polaris id: {}", polarisId);
            return ResponseEntity.notFound().build();
        }

        Map<String, String> params = new HashMap<>();
        params.put(USER_ID, playerId);
        return ResponseEntity.ok(polarisProxyService.fetchStatPentagonFromProxy(params));
    }
}