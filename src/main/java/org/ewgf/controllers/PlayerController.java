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
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final PolarisProxyService polarisProxyService;

    public PlayerController(PlayerService playerService, PolarisProxyService polarisProxyService) {
        this.playerService = playerService;
        this.polarisProxyService = polarisProxyService;
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
        if (query == null || query.isBlank() || query.length() >= 20) return ResponseEntity.badRequest().build();
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

    @GetMapping("/getStatPentagon")
    public ResponseEntity<StatPentagonResponse> getPolarisIdMapping(@RequestParam String polarisId) throws Exception {
        String paddedPolarisId = polarisId;
        if (polarisId.length() < 18) {
            paddedPolarisId = String.format("%18s", polarisId).replace(' ', '0');
        }

        Optional<String> playerId = playerService.getPlayerIdFromPolarisId(paddedPolarisId);
        Map<String, String> params = new HashMap<>();

        if (playerId.isEmpty()) {
            logger.warn("No player found for polaris id: {}", paddedPolarisId);
            return ResponseEntity.notFound().build();
        }
        params.put(USER_ID, playerId.get());
        return ResponseEntity.ok(polarisProxyService.fetchStatPentagonFromProxy(params));
    }
}