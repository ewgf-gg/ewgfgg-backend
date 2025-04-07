package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.ewgf.dtos.RankWinrateChangesDTO;
import org.ewgf.dtos.header_or_footer.TekkenStatsSummaryDTO;
import org.ewgf.dtos.homepage.GlobalCharacterPickRateDTO;
import org.ewgf.dtos.homepage.GlobalWinratesDTO;
import org.ewgf.dtos.homepage.RankDistributionDTO;
import org.ewgf.dtos.statistics_page.CharacterPopularityDTO;
import org.ewgf.dtos.statistics_page.CharacterWinratesDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/top-popularity")
    public ResponseEntity<List<GlobalCharacterPickRateDTO>> getTop5CharacterPopularityStats() throws Exception {
        log.debug("Fetching top 5 popular characters");
        List<GlobalCharacterPickRateDTO> popularity = statisticsService.getHomePageCharacterPopularity();
        return ResponseEntity.ok(popularity);
    }

    @GetMapping("/top-winrates")
    public ResponseEntity<List<GlobalWinratesDTO>> getTop5CharacterWinratesStats() throws Exception {
        log.debug("Fetching top 5 highest winrate characters");
        List<GlobalWinratesDTO> winratesDTO = statisticsService.getHomePageCharacterWinrates();
        return ResponseEntity.ok(winratesDTO);
    }

    @GetMapping("/winrate-changes")
    public ResponseEntity<Map<String, List<RankWinrateChangesDTO>>> getWinrateChanges() {
        log.debug("Fetching character winrate changes");
        Map<String, List<RankWinrateChangesDTO>> groupedChanges = statisticsService.getHomePageWinrateChanges();
        return ResponseEntity.ok(groupedChanges);
    }

    @GetMapping("/rankDistribution")
    public ResponseEntity<Map<Integer, RankDistributionDTO>> getAllRankDistributions() {
        log.debug("Fetching rank distribution for all versions");
        Map<Integer, RankDistributionDTO> result = statisticsService.getAllRankDistributions();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/version-popularity")
    public ResponseEntity<Map<String, CharacterPopularityDTO>> getVersionPopularity() throws Exception {
        log.debug("Fetching popularity stats for all game versions");
        Map<String, CharacterPopularityDTO> popularity = statisticsService.getAllVersionPopularity();
        return ResponseEntity.ok(popularity);
    }

    @GetMapping("/version-winrates")
    public ResponseEntity<Map<String, CharacterWinratesDTO>> getVersionWinrates() throws Exception {
        log.debug("Fetching winrates for all game versions");
        Map<String, CharacterWinratesDTO> winrates = statisticsService.getAllVersionWinrates();
        return ResponseEntity.ok(winrates);
    }

    @GetMapping("/allWinrateChanges")
    public ResponseEntity<Map<String, List<RankWinrateChangesDTO>>> getAllWinrateChanges() {
        log.debug("Fetching all character winrate changes");
        Map<String, List<RankWinrateChangesDTO>> groupedChanges = statisticsService.getAllWinrateChanges();
        return ResponseEntity.ok(groupedChanges);
    }

    private TekkenStatsSummaryDTO convertToDTO(TekkenStatsSummary tekkenStatsSummary) {
        TekkenStatsSummaryDTO dto = new TekkenStatsSummaryDTO();
        dto.setTotalPlayers(tekkenStatsSummary.getTotalPlayers());
        dto.setTotalReplays(tekkenStatsSummary.getTotalReplays());
        return dto;
    }
}