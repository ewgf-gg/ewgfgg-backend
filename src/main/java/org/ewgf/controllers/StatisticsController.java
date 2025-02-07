package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.ewgf.utils.TekkenDataMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.ewgf.interfaces.*;
import org.ewgf.dtos.*;
import org.ewgf.models.TekkenStatsSummary;
import org.ewgf.repositories.AggregatedStatisticsRepository;
import org.ewgf.repositories.TekkenStatsSummaryRepository;
import org.ewgf.services.CharacterAnalyticsService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
@Slf4j
public class StatisticsController {
    private final TekkenStatsSummaryRepository tekkenStatsSummaryRepository;
    private final AggregatedStatisticsRepository aggregatedStatisticsRepository;
    private final CharacterAnalyticsService characterAnalyticsService;

    public StatisticsController(
            TekkenStatsSummaryRepository tekkenStatsSummaryRepository,
            AggregatedStatisticsRepository aggregatedStatisticsRepository,
            CharacterAnalyticsService characterAnalyticsService)
    {
        this.tekkenStatsSummaryRepository = tekkenStatsSummaryRepository;
        this.aggregatedStatisticsRepository = aggregatedStatisticsRepository;
        this.characterAnalyticsService = characterAnalyticsService;
    }

    @GetMapping("/stats-summary")
    public ResponseEntity<TekkenStatsSummaryDTO> getPlayerCount(HttpServletRequest request) throws InterruptedException
    {
        log.info("Received request for stats summary");
        return tekkenStatsSummaryRepository.getTekkenStatsSummary()
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/version-popularity")
    public ResponseEntity<Map<String, CharacterPopularityDTO>> getVersionPopularity()
    {
        log.info("Fetching popularity stats for all game versions");
        try
        {
            Map<String, CharacterPopularityDTO> popularity = characterAnalyticsService.getAllVersionPopularity();
            return ResponseEntity.ok(popularity);
        }
        catch (Exception e)
        {
            log.error("Error fetching version-specific popularity statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/version-winrates")
    public ResponseEntity<Map<String, CharacterWinratesDTO>> getVersionWinrates()
    {
        log.info("Fetching winrates for all game versions");
        try
        {
            Map<String, CharacterWinratesDTO> winrates = characterAnalyticsService.getAllVersionWinrates();
            return ResponseEntity.ok(winrates);
        }
        catch (Exception e)
        {
            log.error("Error fetching version-specific winrates", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/top-popularity")
    public ResponseEntity<CharacterPopularityDTO> getTop5CharacterPopularityStats() {
        log.info("Fetching top 5 popular characters");
        try {
            CharacterPopularityDTO popularity = characterAnalyticsService.getTopCharacterPopularity();
            return ResponseEntity.ok(popularity);
        } catch (Exception e) {
            log.error("Error fetching character popularity statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/top-winrates")
    public ResponseEntity<CharacterWinratesDTO> getTop5CharacterWinratesStats() {
        log.info("Fetching top 5 highest winrate characters");
        try {
            CharacterWinratesDTO winratesDTO = characterAnalyticsService.getTopCharacterWinrates();
            return ResponseEntity.ok(winratesDTO);
        } catch (Exception e) {
            log.error("Error fetching character popularity statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/gameVersions")
    public ResponseEntity<List<Integer>> getGameVersions(HttpServletRequest request) throws InterruptedException
    {
        log.info("Received request for gameVerions");
        return aggregatedStatisticsRepository.getGameVersions()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rankDistribution")
    public ResponseEntity<Map<Integer, RankDistributionDTO>> getAllRankDistributions() {
        log.info("Fetching rank distribution for all versions");
        Optional<List<Integer>> gameVersions = aggregatedStatisticsRepository.getGameVersions();
        List<RankDistributionProjection> distributions = aggregatedStatisticsRepository.getAllRankDistributions(gameVersions.get());
        Map<Integer, RankDistributionDTO> result = new TreeMap<>(Collections.reverseOrder());

        distributions.forEach(dist -> {
            result.computeIfAbsent(dist.getGameVersion(), k -> new RankDistributionDTO())
                    .addDistribution(dist.getCategory(),
                            new RankDistributionEntry(dist.getRank(), dist.getPercentage()));
        });

        return ResponseEntity.ok(result);
    }

    @GetMapping("/winrate-changes")
    public ResponseEntity<Map<String, List<RankWinrateChangesDTO>>> getWinrateChanges() {
        log.info("Fetching character winrate changes");
        try {
            List<WinrateChangesProjection> projections = aggregatedStatisticsRepository.getWinrateChanges();
            List<RankWinrateChangesDTO> changes = projections.stream()
                    .map(proj -> new RankWinrateChangesDTO(
                            TekkenDataMapper.getCharacterName(proj.getCharacterId()),
                            proj.getChange(),
                            proj.getTrend(),
                            proj.getRankCategory()
                    ))
                    .collect(Collectors.toList());

            Map<String, List<RankWinrateChangesDTO>> groupedChanges = RankWinrateChangesDTO.groupByRankCategory(changes);
            return ResponseEntity.ok(groupedChanges);
        } catch (Exception e) {
            log.error("Error fetching character winrate changes", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private TekkenStatsSummaryDTO convertToDTO(TekkenStatsSummary tekkenStatsSummary) {
        TekkenStatsSummaryDTO dto = new TekkenStatsSummaryDTO();
        dto.setTotalPlayers(tekkenStatsSummary.getTotalPlayers());
        dto.setTotalReplays(tekkenStatsSummary.getTotalReplays());
        return dto;
    }
}