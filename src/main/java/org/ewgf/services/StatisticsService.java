package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;

import org.ewgf.dtos.*;
import org.ewgf.interfaces.RankDistributionProjection;
import org.ewgf.interfaces.WinrateChangesProjection;
import org.springframework.stereotype.Service;
import org.ewgf.interfaces.CharacterAnalyticsProjection;
import org.ewgf.interfaces.CharacterWinrateProjection;
import org.ewgf.utils.TekkenDataMapperUtils;
import org.ewgf.repositories.AggregatedStatisticsRepository;
import java.util.*;
import java.util.stream.Collectors;

import static org.ewgf.utils.Constants.*;

@Service
@Slf4j
public class StatisticsService {

    private final AggregatedStatisticsRepository aggregatedStatisticsRepository;

    public StatisticsService(
            AggregatedStatisticsRepository repository) {
        this.aggregatedStatisticsRepository = repository;
    }

    public Map<String, CharacterWinratesDTO> getAllVersionWinrates() throws Exception {
        List<CharacterWinrateProjection> allStats = aggregatedStatisticsRepository.findAllWinrateStats();

            // First group by version
        Map<Integer, List<CharacterWinrateProjection>> statsByVersion = allStats.stream()
                .collect(Collectors.groupingBy(CharacterWinrateProjection::getGameVersion));

        Map<String, CharacterWinratesDTO> result = new HashMap<>();

        for (Map.Entry<Integer, List<CharacterWinrateProjection>> versionEntry : statsByVersion.entrySet()) {
            String version = String.valueOf(versionEntry.getKey());
            List<CharacterWinrateProjection> versionStats = versionEntry.getValue();

            // Group by rank category
            Map<String, List<CharacterWinrateProjection>> statsByRank = versionStats.stream()
                    .collect(Collectors.groupingBy(CharacterWinrateProjection::getRankCategory));

                // Process each rank range
            RegionalCharacterWinrateDTO allRanks = processWinrateStatsWithRegions(statsByRank.get(ALL_RANKS));
            RegionalCharacterWinrateDTO masterRanks = processWinrateStatsWithRegions(statsByRank.get(MASTER_RANK_CATEGORY));
            RegionalCharacterWinrateDTO advancedRanks = processWinrateStatsWithRegions(statsByRank.get(ADVANCED_RANK_CATEGORY));
            RegionalCharacterWinrateDTO intermediateRanks = processWinrateStatsWithRegions(statsByRank.get(INTERMEDIATE_RANK_CATEGORY));
            RegionalCharacterWinrateDTO beginnerRanks = processWinrateStatsWithRegions(statsByRank.get(BEGINNER_RANK_CATEGORY));

            result.put(version, new CharacterWinratesDTO(allRanks, masterRanks, advancedRanks, intermediateRanks, beginnerRanks));
        }
        return result;
    }

    private RegionalCharacterWinrateDTO processWinrateStatsWithRegions(List<CharacterWinrateProjection> stats) {
        if (stats == null || stats.isEmpty()) {
            return new RegionalCharacterWinrateDTO(new HashMap<>(), new HashMap<>());
        }

        // Group by region first
        Map<String, List<CharacterWinrateProjection>> groupedByRegion = stats.stream()
                .collect(Collectors.groupingBy(CharacterWinrateProjection::getRegionId));

        // Process global stats
        Map<String, Double> globalStats = groupedByRegion.getOrDefault(GLOBAL_REGION, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        stat -> TekkenDataMapperUtils.getCharacterName((stat.getCharacterId())),
                        this::calculateWinrate
                ));

        // Process regional stats
        Map<String, Map<String, Double>> regionalStats = groupedByRegion.entrySet().stream()
                .filter(entry -> !GLOBAL_REGION.equals(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .collect(Collectors.toMap(
                                        stat -> TekkenDataMapperUtils.getCharacterName(stat.getCharacterId()),
                                        this::calculateWinrate
                                ))
                ));
        return new RegionalCharacterWinrateDTO(globalStats, regionalStats);
    }

    public Map<String, CharacterPopularityDTO> getAllVersionPopularity() throws Exception {
        List<CharacterAnalyticsProjection> allStats = aggregatedStatisticsRepository.findAllCharactersByPopularity();

        // First group by version
        Map<Integer, List<CharacterAnalyticsProjection>> statsByVersion = allStats.stream()
                .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getGameVersion));

        Map<String, CharacterPopularityDTO> result = new HashMap<>();

        for (Map.Entry<Integer, List<CharacterAnalyticsProjection>> versionEntry : statsByVersion.entrySet()) {
            String version = String.valueOf(versionEntry.getKey());
            List<CharacterAnalyticsProjection> versionStats = versionEntry.getValue();

            // Group by rank category
            Map<String, List<CharacterAnalyticsProjection>> statsByRank = versionStats.stream()
                    .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getRankCategory));

            RegionalCharacterPopularityDTO allRanks = processStats(statsByRank.get(ALL_RANKS));
            RegionalCharacterPopularityDTO masterRanks = processStats(statsByRank.get(MASTER_RANK_CATEGORY));
            RegionalCharacterPopularityDTO advancedRanks = processStats(statsByRank.get(ADVANCED_RANK_CATEGORY));
            RegionalCharacterPopularityDTO intermediateRanks = processStats(statsByRank.get(INTERMEDIATE_RANK_CATEGORY));
            RegionalCharacterPopularityDTO beginnerRanks = processStats(statsByRank.get(BEGINNER_RANK_CATEGORY));
            result.put(version, new CharacterPopularityDTO(allRanks, masterRanks, advancedRanks, intermediateRanks, beginnerRanks));
            }
            return result;
    }

    private RegionalCharacterPopularityDTO processStats(List<CharacterAnalyticsProjection> stats) {
        if (stats == null || stats.isEmpty()) {
            return new RegionalCharacterPopularityDTO(new HashMap<>(), new HashMap<>());
        }

        Map<String, List<CharacterAnalyticsProjection>> groupedByRegion = stats.stream()
                .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getRegionId));

        // Process global stats
        Map<String, Long> globalStats = groupedByRegion.getOrDefault(GLOBAL_REGION, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        stat -> TekkenDataMapperUtils.getCharacterName(stat.getCharacterId()),
                        CharacterAnalyticsProjection::getTotalBattles
                ));

        // Process regional stats
        Map<String, Map<String, Long>> regionalStats = groupedByRegion.entrySet().stream()
                .filter(entry -> !GLOBAL_REGION.equals(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .collect(Collectors.toMap(
                                        stat -> TekkenDataMapperUtils.getCharacterName(stat.getCharacterId()),
                                        CharacterAnalyticsProjection::getTotalBattles
                                ))
                ));
        return new RegionalCharacterPopularityDTO(globalStats, regionalStats);
    }

    public CharacterPopularityDTO getHomePageCharacterPopularity() throws Exception {
        List<CharacterAnalyticsProjection> stats = aggregatedStatisticsRepository.findTopCharactersByPopularity();
        // Group by rank category
        Map<String, List<CharacterAnalyticsProjection>> statsByRank = stats.stream()
                    .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getRankCategory));

        Map<String, Long> masterRankStats = processTopStats(statsByRank.getOrDefault(MASTER_RANK_CATEGORY, Collections.emptyList()));
        Map<String, Long> advcancedRankStats = processTopStats(statsByRank.getOrDefault(ADVANCED_RANK_CATEGORY, Collections.emptyList()));
        Map<String, Long> intermediateRankStats = processTopStats(statsByRank.getOrDefault(INTERMEDIATE_RANK_CATEGORY, Collections.emptyList()));
        Map<String, Long> beginnerRankStats = processTopStats(statsByRank.getOrDefault(BEGINNER_RANK_CATEGORY, Collections.emptyList()));

        // Since we're only returning top characters (for the front page) , we only need global stats
        RegionalCharacterPopularityDTO masterRanks = new RegionalCharacterPopularityDTO(masterRankStats, new HashMap<>());
        RegionalCharacterPopularityDTO advancedRanks = new RegionalCharacterPopularityDTO(advcancedRankStats, new HashMap<>());
        RegionalCharacterPopularityDTO intermediateRanks = new RegionalCharacterPopularityDTO(intermediateRankStats, new HashMap<>());
        RegionalCharacterPopularityDTO beginnerRanks = new RegionalCharacterPopularityDTO(beginnerRankStats, new HashMap<>());
        return new CharacterPopularityDTO(masterRanks, advancedRanks, intermediateRanks, beginnerRanks);
    }

    public CharacterWinratesDTO getHomePageCharacterWinrates() throws Exception {
        List<CharacterAnalyticsProjection> stats = aggregatedStatisticsRepository.findTopCharactersByWinrate();
        // Group by rank category
        Map<String, List<CharacterAnalyticsProjection>> statsByRank = stats.stream()
                .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getRankCategory));

        Map<String, Double> masterRankStats = processTopWinrates(statsByRank.getOrDefault(MASTER_RANK_CATEGORY, Collections.emptyList()));
        Map<String, Double> advancedRankStats = processTopWinrates(statsByRank.getOrDefault(ADVANCED_RANK_CATEGORY, Collections.emptyList()));
        Map<String, Double> intermediateRankStats = processTopWinrates(statsByRank.getOrDefault(INTERMEDIATE_RANK_CATEGORY, Collections.emptyList()));
        Map<String, Double> beginnnerRankStats = processTopWinrates(statsByRank.getOrDefault(BEGINNER_RANK_CATEGORY, Collections.emptyList()));

        // Since we're only returning top characters (for the front page) , we only need global stats
        RegionalCharacterWinrateDTO masterRanks = new RegionalCharacterWinrateDTO(masterRankStats, new HashMap<>());
        RegionalCharacterWinrateDTO highRank = new RegionalCharacterWinrateDTO(advancedRankStats, new HashMap<>());
        RegionalCharacterWinrateDTO mediumRank = new RegionalCharacterWinrateDTO(intermediateRankStats, new HashMap<>());
        RegionalCharacterWinrateDTO lowRank = new RegionalCharacterWinrateDTO(beginnnerRankStats, new HashMap<>());
        return new CharacterWinratesDTO(masterRanks,highRank, mediumRank, lowRank);
    }

    public Optional<List<Integer>> getGameVersions() {
        return aggregatedStatisticsRepository.getGameVersions();
    }

    public Map<Integer, RankDistributionDTO> getAllRankDistributions() {
        Optional<List<Integer>> gameVersions = aggregatedStatisticsRepository.getGameVersions();
        List<RankDistributionProjection> distributions = aggregatedStatisticsRepository.getAllRankDistributions(gameVersions.get());
        Map<Integer, RankDistributionDTO> result = new TreeMap<>(Collections.reverseOrder());

        distributions.forEach(dist -> {
            result.computeIfAbsent(dist.getGameVersion(), k -> new RankDistributionDTO())
                    .addDistribution(dist.getCategory(),
                            new RankDistributionEntry(dist.getRank(), dist.getPercentage()));
        });
        return result;
    }

    public Map<String, List<RankWinrateChangesDTO>> getHomePageWinrateChanges() {
        List<WinrateChangesProjection> projections = aggregatedStatisticsRepository.getWinrateChanges();
        List<RankWinrateChangesDTO> changes = projections.stream()
                .map(proj -> new RankWinrateChangesDTO(
                        TekkenDataMapperUtils.getCharacterName(proj.getCharacterId()),
                        proj.getChange(),
                        proj.getTrend(),
                        proj.getRankCategory()))
                .collect(Collectors.toList());
        return RankWinrateChangesDTO.groupByRankCategory(changes);
    }

    public Map<String, List<RankWinrateChangesDTO>> getAllWinrateChanges() {
        List<WinrateChangesProjection> projections = aggregatedStatisticsRepository.getAllWinrateChanges();
        List<RankWinrateChangesDTO> changes = projections.stream()
                .map(proj -> new RankWinrateChangesDTO(
                        TekkenDataMapperUtils.getCharacterName(proj.getCharacterId()),
                        proj.getChange(),
                        proj.getTrend(),
                        proj.getRankCategory()))
                .collect(Collectors.toList());
        return RankWinrateChangesDTO.groupByRankCategory(changes);
    }


    private Map<String, Long> processTopStats(List<CharacterAnalyticsProjection> stats) {
        if (stats == null) return new HashMap<>();
        return stats.stream().collect(Collectors.toMap(
                stat -> TekkenDataMapperUtils.getCharacterName(stat.getCharacterId()),
                CharacterAnalyticsProjection::getTotalBattles
        ));
    }
    private Map<String, Double> processTopWinrates(List<CharacterAnalyticsProjection> stats) {
        if (stats == null) return new HashMap<>();
        return stats.stream().collect(Collectors.toMap(
                stat -> TekkenDataMapperUtils.getCharacterName(stat.getCharacterId()),
                CharacterAnalyticsProjection::getWinratePercentage
        ));
    }

    private double calculateWinrate(CharacterWinrateProjection stat) {
        long totalMatches = stat.getTotalWins() + stat.getTotalLosses();
        return totalMatches > 0
                ? (stat.getTotalWins() * 100.0) / totalMatches
                : 0.0;
    }
}