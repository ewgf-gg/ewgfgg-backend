package org.tekkenstats.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tekkenstats.aggregations.AggregatedStatistic;
import org.tekkenstats.aggregations.AggregatedStatisticId;
import org.tekkenstats.aggregations.PlayerCharacterData;
import org.tekkenstats.events.ReplayProcessingCompletedEvent;
import org.tekkenstats.repositories.AggregatedStatisticsRepository;
import org.tekkenstats.repositories.CharacterStatsRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final CharacterStatsRepository characterStatsRepository;
    private final AggregatedStatisticsRepository aggregatedStatisticsRepository;
    private final Executor statisticsExecutor;
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public StatisticsService(
            CharacterStatsRepository characterStatsRepository,
            AggregatedStatisticsRepository aggregatedStatisticsRepository,
            @Qualifier("statisticsThreadExecutor") Executor statisticsExecutor) {
        this.characterStatsRepository = characterStatsRepository;
        this.aggregatedStatisticsRepository = aggregatedStatisticsRepository;
        this.statisticsExecutor = statisticsExecutor;
    }

    @EventListener
    @Async("statisticsThreadExecutor")
    public void onReplayDataProcessed(ReplayProcessingCompletedEvent event)
    {
        if (!isProcessing.compareAndSet(false, true))
        {
            logger.info("Statistics computation already in progress");
            return;
        }

        try
        {
            logger.info("Computing statistics");
            Optional<List<Integer>> gameVersions = fetchGameVersions();
            if (gameVersions.isPresent())
            {
                for (int gameVersion : gameVersions.get())
                {
                    processGameVersionStatistics(gameVersion);
                }
            } else
            {
                logger.warn("No game versions found.");
            }
        } catch (Exception e)
        {
            logger.error("Error computing statistics: ", e);
        } finally
        {
            isProcessing.set(false);
        }
    }

    private Optional<List<Integer>> fetchGameVersions() {
        return characterStatsRepository.findAllGameVersions();
    }

    private void processGameVersionStatistics(int gameVersion) {
        logger.info("Processing statistics for game version: " + gameVersion);
        List<Object[]> allStats = characterStatsRepository.findAllStatsByGameVersion(gameVersion);

        // Load existing statistics for 'standard' category
        List<AggregatedStatistic> existingStatsStandard = aggregatedStatisticsRepository.findByIdGameVersionAndIdCategory(gameVersion, "standard");
        Map<AggregatedStatisticId, AggregatedStatistic> existingStatsMapStandard = existingStatsStandard.stream()
                .collect(Collectors.toMap(AggregatedStatistic::getId, Function.identity()));

        Map<String, PlayerCharacterData> playerMainCharacters = identifyPlayerMainCharacters(allStats);
        Map<AggregatedStatisticId, AggregatedStatistic> mainAggregatedData = aggregateStatistics(playerMainCharacters, "standard", gameVersion, existingStatsMapStandard);
        saveAggregatedStatistics(mainAggregatedData.values());

        // Load existing statistics for 'overall' category
        List<AggregatedStatistic> existingStatsOverall = aggregatedStatisticsRepository.findByIdGameVersionAndIdCategory(gameVersion, "overall");
        Map<AggregatedStatisticId, AggregatedStatistic> existingStatsMapOverall = existingStatsOverall.stream()
                .collect(Collectors.toMap(AggregatedStatistic::getId, Function.identity()));

        Map<String, List<PlayerCharacterData>> allPlayerCharacters = getAllPlayerCharacters(allStats);
        Map<AggregatedStatisticId, AggregatedStatistic> overallAggregatedData = aggregateOverallStatistics(allPlayerCharacters, gameVersion, existingStatsMapOverall);
        saveAggregatedStatistics(overallAggregatedData.values());
    }

    private Map<String, PlayerCharacterData> identifyPlayerMainCharacters(List<Object[]> stats) {
        Map<String, PlayerCharacterData> playerDataMap = new HashMap<>();
        for (Object[] row : stats)
        {
            // Skip if the row doesn't have all required elements
            if (row[5] == null || row[6] == null)
            {
                continue;
            }

            try
            {
                String playerId = (String) row[0];
                String characterId = (String) row[1];
                int danRank = ((Number) row[2]).intValue();
                int wins = ((Number) row[3]).intValue();
                int losses = ((Number) row[4]).intValue();
                int regionId = ((Number) row[5]).intValue();
                int areaId = ((Number) row[6]).intValue();
                int totalPlays = wins + losses;

                PlayerCharacterData currentData = playerDataMap.get(playerId);
                if (currentData == null || totalPlays > currentData.getTotalPlays())
                {
                    playerDataMap.put(playerId, new PlayerCharacterData(characterId, danRank, wins, losses, totalPlays, regionId, areaId));
                }
            } catch (ClassCastException | NullPointerException e)
            {
                logger.error("Error computing statistics for main characters: ", e);
            }
        }
        return playerDataMap;
    }

    private Map<String, List<PlayerCharacterData>> getAllPlayerCharacters(List<Object[]> stats) {
        Map<String, List<PlayerCharacterData>> playerDataMap = new HashMap<>();
        for (Object[] row : stats)
        {
            if (row[5] == null || row[6] == null)
            {
                continue;
            }

            String playerId = (String) row[0];
            String characterId = (String) row[1];
            int danRank = ((Number) row[2]).intValue();
            int wins = ((Number) row[3]).intValue();
            int losses = ((Number) row[4]).intValue();
            int regionId = ((Number) row[5]).intValue();  // New field
            int areaId = ((Number) row[6]).intValue();    // New field
            int totalPlays = wins + losses;

            playerDataMap.computeIfAbsent(playerId, k -> new ArrayList<>())
                    .add(new PlayerCharacterData(characterId, danRank, wins, losses, totalPlays, regionId, areaId));
        }
        return playerDataMap;
    }

    private Map<AggregatedStatisticId, AggregatedStatistic> aggregateStatistics(
            Map<String, PlayerCharacterData> playerCharacters,
            String category,
            int gameVersion,
            Map<AggregatedStatisticId, AggregatedStatistic> existingStats) {

        Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData = new HashMap<>();
        Map<AggregatedStatisticId, Set<String>> playersPerStat = new HashMap<>();

        for (Map.Entry<String, PlayerCharacterData> entry : playerCharacters.entrySet()) {
            String playerId = entry.getKey();
            PlayerCharacterData data = entry.getValue();

            // Updated constructor call to include region and area IDs
            AggregatedStatisticId id = new AggregatedStatisticId(
                    gameVersion,
                    data.getCharacterId(),
                    data.getDanRank(),
                    category,
                    data.getRegionID(),  // New field
                    data.getAreaID()      // New field
            );

            AggregatedStatistic stat = existingStats.get(id);
            if (stat == null) {
                stat = new AggregatedStatistic(id);
                stat.setComputedAt(LocalDateTime.now());
            } else {
                if (!aggregatedData.containsKey(id)) {
                    stat.setTotalWins(0);
                    stat.setTotalLosses(0);
                    stat.setTotalPlayers(0);
                    stat.setTotalReplays(0);
                    stat.setComputedAt(LocalDateTime.now());
                }
            }

            stat.setTotalWins(stat.getTotalWins() + data.getWins());
            stat.setTotalLosses(stat.getTotalLosses() + data.getLosses());
            stat.setTotalReplays(stat.getTotalReplays() + data.getTotalPlays());

            playersPerStat.computeIfAbsent(id, k -> new HashSet<>()).add(playerId);

            aggregatedData.put(id, stat);
        }

        // Update totalPlayers based on unique player counts
        for (Map.Entry<AggregatedStatisticId, AggregatedStatistic> entry : aggregatedData.entrySet()) {
            AggregatedStatisticId id = entry.getKey();
            AggregatedStatistic stat = entry.getValue();
            Set<String> players = playersPerStat.get(id);
            stat.setTotalPlayers(players.size());
        }

        return aggregatedData;
    }

    private Map<AggregatedStatisticId, AggregatedStatistic> aggregateOverallStatistics(
            Map<String, List<PlayerCharacterData>> allPlayerCharacters,
            int gameVersion,
            Map<AggregatedStatisticId, AggregatedStatistic> existingStats) {

        Map<AggregatedStatisticId, AggregatedStatistic> overallData = new HashMap<>();
        Map<AggregatedStatisticId, Set<String>> playersPerStat = new HashMap<>();

        for (Map.Entry<String, List<PlayerCharacterData>> entry : allPlayerCharacters.entrySet()) {
            String playerId = entry.getKey();
            List<PlayerCharacterData> playerCharacters = entry.getValue();

            for (PlayerCharacterData data : playerCharacters) {
                // Updated constructor call to include region and area IDs
                AggregatedStatisticId id = new AggregatedStatisticId(
                        gameVersion,
                        data.getCharacterId(),
                        data.getDanRank(),
                        "overall",
                        data.getRegionID(),  // New field
                        data.getAreaID()      // New field
                );

                AggregatedStatistic stat = existingStats.get(id);
                if (stat == null) {
                    stat = new AggregatedStatistic(id);
                    stat.setComputedAt(LocalDateTime.now());
                } else {
                    if (!overallData.containsKey(id)) {
                        stat.setTotalWins(0);
                        stat.setTotalLosses(0);
                        stat.setTotalPlayers(0);
                        stat.setTotalReplays(0);
                        stat.setComputedAt(LocalDateTime.now());
                    }
                }

                stat.setTotalWins(stat.getTotalWins() + data.getWins());
                stat.setTotalLosses(stat.getTotalLosses() + data.getLosses());
                stat.setTotalReplays(stat.getTotalReplays() + data.getTotalPlays());

                playersPerStat.computeIfAbsent(id, k -> new HashSet<>()).add(playerId);

                overallData.put(id, stat);
            }
        }

        // Update totalPlayers based on unique player counts
        for (Map.Entry<AggregatedStatisticId, AggregatedStatistic> entry : overallData.entrySet()) {
            AggregatedStatisticId id = entry.getKey();
            AggregatedStatistic stat = entry.getValue();
            Set<String> players = playersPerStat.get(id);
            stat.setTotalPlayers(players.size());
        }

        return overallData;
    }

    private void saveAggregatedStatistics(Collection<AggregatedStatistic> statistics) {
        aggregatedStatisticsRepository.saveAll(statistics);
    }
}