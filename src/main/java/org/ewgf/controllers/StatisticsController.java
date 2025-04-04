package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.ewgf.dtos.*;
import org.ewgf.models.TekkenStatsSummary;
import org.ewgf.repositories.TekkenStatsSummaryRepository;
import org.ewgf.services.StatisticsService;

import java.util.*;

@RestController
@RequestMapping("/statistics")
@Slf4j
public class StatisticsController {
    private final TekkenStatsSummaryRepository tekkenStatsSummaryRepository;
    private final StatisticsService statisticsService;

    public StatisticsController(
            TekkenStatsSummaryRepository tekkenStatsSummaryRepository,
            StatisticsService statisticsService)
    {
        this.tekkenStatsSummaryRepository = tekkenStatsSummaryRepository;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/stats-summary")
    public ResponseEntity<TekkenStatsSummaryDTO> getPlayerCount(HttpServletRequest request) throws InterruptedException {
        log.info("Received request for stats summary");
        return tekkenStatsSummaryRepository.getTekkenStatsSummary()
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/version-popularity")
    public ResponseEntity<Map<String, CharacterPopularityDTO>> getVersionPopularity() throws Exception {
        log.info("Fetching popularity stats for all game versions");
        Map<String, CharacterPopularityDTO> popularity = statisticsService.getAllVersionPopularity();
        return ResponseEntity.ok(popularity);
    }

    @GetMapping("/version-winrates")
    public ResponseEntity<Map<String, CharacterWinratesDTO>> getVersionWinrates() throws Exception {
        log.info("Fetching winrates for all game versions");
        Map<String, CharacterWinratesDTO> winrates = statisticsService.getAllVersionWinrates();
        return ResponseEntity.ok(winrates);
    }

    @GetMapping("/top-popularity")
    public ResponseEntity<CharacterPopularityDTO> getTop5CharacterPopularityStats() throws Exception {
        log.info("Fetching top 5 popular characters");
        CharacterPopularityDTO popularity = statisticsService.getTopCharacterPopularity();
        return ResponseEntity.ok(popularity);
    }

    @GetMapping("/top-winrates")
    public ResponseEntity<CharacterWinratesDTO> getTop5CharacterWinratesStats() throws Exception {
        log.info("Fetching top 5 highest winrate characters");
        CharacterWinratesDTO winratesDTO = statisticsService.getTopCharacterWinrates();
        return ResponseEntity.ok(winratesDTO);
    }

    @GetMapping("/gameVersions")
    public ResponseEntity<List<Integer>> getGameVersions(HttpServletRequest request) throws InterruptedException {
        log.info("Received request for gameVersions");
        return statisticsService.getGameVersions()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rankDistribution")
    public ResponseEntity<Map<Integer, RankDistributionDTO>> getAllRankDistributions() {
        log.info("Fetching rank distribution for all versions");
        Map<Integer, RankDistributionDTO> result = statisticsService.getAllRankDistributions();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/winrate-changes")
    public ResponseEntity<Map<String, List<RankWinrateChangesDTO>>> getWinrateChanges() {
        log.info("Fetching character winrate changes");
        Map<String, List<RankWinrateChangesDTO>> groupedChanges = statisticsService.getWinrateChanges();
        return ResponseEntity.ok(groupedChanges);
    }

    private TekkenStatsSummaryDTO convertToDTO(TekkenStatsSummary tekkenStatsSummary) {
        TekkenStatsSummaryDTO dto = new TekkenStatsSummaryDTO();
        dto.setTotalPlayers(tekkenStatsSummary.getTotalPlayers());
        dto.setTotalReplays(tekkenStatsSummary.getTotalReplays());
        return dto;
    }
}