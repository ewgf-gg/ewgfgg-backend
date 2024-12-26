package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.ewgf.dtos.CharacterPopularityDTO;

import org.ewgf.dtos.CharacterWinratesDTO;

import org.ewgf.dtos.RegionalCharacterPopularityDTO;

import org.ewgf.dtos.RegionalCharacterWinrateDTO;
import org.ewgf.interfaces.CharacterAnalyticsProjection;
import org.ewgf.interfaces.CharacterWinrateProjection;

import org.ewgf.mappers.EnumsMapper;

import org.ewgf.repositories.AggregatedStatisticsRepository;


import java.util.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class CharacterAnalyticsService {
    private final AggregatedStatisticsRepository repository;
    private final EnumsMapper enumsMapper;

    public CharacterAnalyticsService(
            AggregatedStatisticsRepository repository,
            EnumsMapper enumsMapper) {
        this.repository = repository;
        this.enumsMapper = enumsMapper;
    }

    public Map<String, CharacterWinratesDTO> getAllVersionWinrates() throws Exception {
        try {
            List<CharacterWinrateProjection> allStats = repository.findAllWinrateStats();

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
                RegionalCharacterWinrateDTO allRanks = processWinrateStatsWithRegions(statsByRank.get("allRanks"));
                RegionalCharacterWinrateDTO highRank = processWinrateStatsWithRegions(statsByRank.get("highRank"));
                RegionalCharacterWinrateDTO mediumRank = processWinrateStatsWithRegions(statsByRank.get("mediumRank"));
                RegionalCharacterWinrateDTO lowRank = processWinrateStatsWithRegions(statsByRank.get("lowRank"));

                result.put(version, new CharacterWinratesDTO(allRanks, highRank, mediumRank, lowRank));
            }

            return result;
        } catch (Exception e) {
            log.error("Error calculating version-specific winrates", e);
            throw new Exception("Failed to retrieve version-specific winrates", e);
        }
    }

    private RegionalCharacterWinrateDTO processWinrateStatsWithRegions(List<CharacterWinrateProjection> stats) {
        if (stats == null || stats.isEmpty()) {
            return new RegionalCharacterWinrateDTO(new HashMap<>(), new HashMap<>());
        }

        // Group by region first
        Map<String, List<CharacterWinrateProjection>> groupedByRegion = stats.stream()
                .collect(Collectors.groupingBy(CharacterWinrateProjection::getRegionId));

        // Process global stats
        Map<String, Double> globalStats = groupedByRegion.getOrDefault("Global", Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        stat -> enumsMapper.getCharacterName(stat.getCharacterId()),
                        this::calculateWinrate
                ));

        // Process regional stats
        Map<String, Map<String, Double>> regionalStats = groupedByRegion.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("Global"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .collect(Collectors.toMap(
                                        stat -> enumsMapper.getCharacterName(stat.getCharacterId()),
                                        this::calculateWinrate
                                ))
                ));

        return new RegionalCharacterWinrateDTO(globalStats, regionalStats);
    }

    public Map<String, CharacterPopularityDTO> getAllVersionPopularity() throws Exception {
        try {
            List<CharacterAnalyticsProjection> allStats = repository.findAllCharactersByPopularity();

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

                RegionalCharacterPopularityDTO allRanks = processStats(statsByRank.get("allRanks"));
                RegionalCharacterPopularityDTO highRank = processStats(statsByRank.get("highRank"));
                RegionalCharacterPopularityDTO mediumRank = processStats(statsByRank.get("mediumRank"));
                RegionalCharacterPopularityDTO lowRank = processStats(statsByRank.get("lowRank"));

                result.put(version, new CharacterPopularityDTO(allRanks, highRank, mediumRank, lowRank));
            }

            return result;
        } catch (Exception e) {
            log.error("Error calculating version-specific popularity", e);
            throw new Exception("Failed to retrieve version-specific popularity", e);
        }
    }

    private RegionalCharacterPopularityDTO processStats(List<CharacterAnalyticsProjection> stats) {
        if (stats == null || stats.isEmpty()) {
            return new RegionalCharacterPopularityDTO(new HashMap<>(), new HashMap<>());
        }

        Map<String, List<CharacterAnalyticsProjection>> groupedByRegion = stats.stream()
                .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getRegionId));

        // Process global stats
        Map<String, Long> globalStats = groupedByRegion.getOrDefault("Global", Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        stat -> enumsMapper.getCharacterName(stat.getCharacterId()),
                        CharacterAnalyticsProjection::getTotalBattles
                ));

        // Process regional stats
        Map<String, Map<String, Long>> regionalStats = groupedByRegion.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("Global"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .collect(Collectors.toMap(
                                        stat -> enumsMapper.getCharacterName(stat.getCharacterId()),
                                        CharacterAnalyticsProjection::getTotalBattles
                                ))
                ));

        return new RegionalCharacterPopularityDTO(globalStats, regionalStats);
    }

    public CharacterPopularityDTO getTopCharacterPopularity() throws Exception
    {
        try {
            List<CharacterAnalyticsProjection> stats = repository.findTopCharactersByPopularity();

            // Group by rank category
            Map<String, List<CharacterAnalyticsProjection>> statsByRank = stats.stream()
                    .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getRankCategory));


            Map<String, Long> highRankStats = processTopStats(statsByRank.get("highRank"));
            Map<String, Long> mediumRankStats = processTopStats(statsByRank.get("mediumRank"));
            Map<String, Long> lowRankStats = processTopStats(statsByRank.get("lowRank"));

            // Since we're only getting top characters, we only need global stats
            RegionalCharacterPopularityDTO highRank = new RegionalCharacterPopularityDTO(highRankStats, new HashMap<>());
            RegionalCharacterPopularityDTO mediumRank = new RegionalCharacterPopularityDTO(mediumRankStats, new HashMap<>());
            RegionalCharacterPopularityDTO lowRank = new RegionalCharacterPopularityDTO(lowRankStats, new HashMap<>());

            return new CharacterPopularityDTO(highRank, mediumRank, lowRank);
        } catch (Exception e) {
            log.error("Error calculating top character popularity", e);
            throw new Exception("Failed to retrieve top character popularity", e);
        }
    }

    public CharacterWinratesDTO getTopCharacterWinrates() throws Exception {
        try
        {
            List<CharacterAnalyticsProjection> stats = repository.findTopCharactersByWinrate();

            // Group by rank category
            Map<String, List<CharacterAnalyticsProjection>> statsByRank = stats.stream()
                    .collect(Collectors.groupingBy(CharacterAnalyticsProjection::getRankCategory));

            // For each rank, if fetchTop5 is true, take only top 5 characters
            Map<String, Double> highRankStats = processTopWinrates(statsByRank.get("highRank"));
            Map<String, Double> mediumRankStats = processTopWinrates(statsByRank.get("mediumRank"));
            Map<String, Double> lowRankStats = processTopWinrates(statsByRank.get("lowRank"));

            // Since we're only getting top characters, we only need global stats
            RegionalCharacterWinrateDTO highRank = new RegionalCharacterWinrateDTO(highRankStats, new HashMap<>());
            RegionalCharacterWinrateDTO mediumRank = new RegionalCharacterWinrateDTO(mediumRankStats, new HashMap<>());
            RegionalCharacterWinrateDTO lowRank = new RegionalCharacterWinrateDTO(lowRankStats, new HashMap<>());

            return new CharacterWinratesDTO(highRank, mediumRank, lowRank);
        } catch (Exception e) {
            log.error("Error calculating top character winrates", e);
            throw new Exception("Failed to retrieve top character winrates", e);
        }
    }

    private Map<String, Long> processTopStats(List<CharacterAnalyticsProjection> stats) {

        Stream<CharacterAnalyticsProjection> stream = stats.stream();

        return stream.collect(Collectors.toMap(
                stat -> enumsMapper.getCharacterName(stat.getCharacterId()),
                CharacterAnalyticsProjection::getTotalBattles
        ));
    }

    private Map<String, Double> processTopWinrates(List<CharacterAnalyticsProjection> stats) {

        Stream<CharacterAnalyticsProjection> stream = stats.stream();

        return stream.collect(Collectors.toMap(
                stat -> enumsMapper.getCharacterName(stat.getCharacterId()),
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