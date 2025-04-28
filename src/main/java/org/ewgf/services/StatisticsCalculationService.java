package org.ewgf.services;

import org.ewgf.repositories.TekkenStatsSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ewgf.aggregations.AggregatedStatistic;
import org.ewgf.aggregations.AggregatedStatisticId;
import org.ewgf.aggregations.IndividualStatistic;
import org.ewgf.events.ReplayProcessingCompletedEvent;
import org.ewgf.repositories.AggregatedStatisticsRepository;
import org.ewgf.repositories.CharacterStatsRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ewgf.utils.Constants.OVERALL_CATEGORY;
import static org.ewgf.utils.Constants.STANDARD_CATEGORY;

@Service
public class StatisticsCalculationService {
    private final CharacterStatsRepository characterStatsRepository;
    private final AggregatedStatisticsRepository aggregatedStatisticsRepository;
    private final TekkenStatsSummaryRepository tekkenStatsSummaryRepository;
    private final Executor statisticsExecutor;
    private static final Logger logger = LoggerFactory.getLogger(StatisticsCalculationService.class);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public StatisticsCalculationService(
            CharacterStatsRepository characterStatsRepository,
            AggregatedStatisticsRepository aggregatedStatisticsRepository,
            TekkenStatsSummaryRepository tekkenStatsSummaryRepository,
            @Qualifier("statisticsThreadExecutor") Executor statisticsExecutor) {
        this.characterStatsRepository = characterStatsRepository;
        this.aggregatedStatisticsRepository = aggregatedStatisticsRepository;
        this.tekkenStatsSummaryRepository = tekkenStatsSummaryRepository;
        this.statisticsExecutor = statisticsExecutor;
    }

    @EventListener
    @Async("statisticsThreadExecutor")
    public void onReplayDataProcessed(ReplayProcessingCompletedEvent event) {
        if (!acquireProcessingLock()) {
            logger.info("Statistics computation already in progress.");
            return;
        }

        try {
            logger.info("Computing statistics for game versions: {} ", event.getGameVersions());
            processGameVersions(event.getGameVersions());
            tekkenStatsSummaryRepository.updateTotalPlayersCount();
        } catch (Exception e) {
            logger.error("Error computing statistics: ", e);
        } finally {
            releaseProcessingLock();
            logger.info("Statistics computation done.");
        }
    }

    private void processGameVersions(Set<Integer> gameVersions) {
        for (int gameVersion : gameVersions) {
            processGameVersionStatistics(gameVersion);
        }
    }

    private void processGameVersionStatistics(int gameVersion) {
        logger.info("Processing statistics for game version: {}", gameVersion);
        List<Object[]> allStats = characterStatsRepository.findAllStatsByGameVersion(gameVersion);
        processStatisticsByCategory(allStats,gameVersion, STANDARD_CATEGORY);
        processStatisticsByCategory(allStats,gameVersion, OVERALL_CATEGORY);
    }

    private void processStatisticsByCategory(List<Object[]> allStats, int gameVersion, String category) {
        Map<AggregatedStatisticId, AggregatedStatistic> existingStatsMap =
                loadExistingStatistics(gameVersion, category);

        if (category.equals(STANDARD_CATEGORY)) {
            Map<String, IndividualStatistic> playerMainCharacters = identifyPlayerMainCharacters(allStats);
            Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData =
                    aggregateStatistics(playerMainCharacters, category, gameVersion, existingStatsMap);
            saveAggregatedStatistics(aggregatedData.values());
        } else {
            Map<String, List<IndividualStatistic>> allPlayerCharacters = getAllPlayerCharacters(allStats);
            Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData =
                    aggregateOverallStatistics(allPlayerCharacters, gameVersion, existingStatsMap);
            saveAggregatedStatistics(aggregatedData.values());
        }
    }

    private Map<String, IndividualStatistic> identifyPlayerMainCharacters(List<Object[]> stats) {
        Map<String, IndividualStatistic> playerDataMap = new HashMap<>();

        for (Object[] row : stats) {
            // Skip records with missing region
            if (row[5] == null) continue;
            try {
                String playerId = (String) row[0];
                IndividualStatistic characterData = extractFromDbRow(row);
                updateMainCharacter(playerDataMap, playerId, characterData);
            } catch (ClassCastException | NullPointerException e) {
                logger.error("Error processing row for main character statistics: ", e);
            }
        }

        return playerDataMap;
    }

    private void updateMainCharacter(
            Map<String, IndividualStatistic> playerDataMap,
            String playerId,
            IndividualStatistic newData) {

        IndividualStatistic currentData = playerDataMap.get(playerId);

        if (currentData == null ||
                newData.getDanRank() > currentData.getDanRank() ||
                (newData.getDanRank() == currentData.getDanRank() &&
                        newData.getTotalPlays() > currentData.getTotalPlays())) {
            playerDataMap.put(playerId, newData);
        }
    }

    private Map<String, List<IndividualStatistic>> getAllPlayerCharacters(List<Object[]> stats) {
        Map<String, List<IndividualStatistic>> playerDataMap = new HashMap<>();

        for (Object[] row : stats) {
            // Skip records with missing region
            if (row[5] == null) continue;

            try {
                String playerId = (String) row[0];
                IndividualStatistic characterData = extractFromDbRow(row);

                // Add this character to the player's list
                playerDataMap.computeIfAbsent(playerId, k -> new ArrayList<>())
                        .add(characterData);
            } catch (ClassCastException | NullPointerException e) {
                logger.error("Error processing row for overall character statistics: ", e);
            }
        }
        return playerDataMap;
    }

    private IndividualStatistic extractFromDbRow(Object[] row) {
        String characterId = (String) row[1];
        int danRank = ((Number) row[2]).intValue();
        int wins = ((Number) row[3]).intValue();
        int losses = ((Number) row[4]).intValue();
        int regionId = ((Number) row[5]).intValue();
        int totalPlays = wins + losses;
        return new IndividualStatistic(characterId, danRank, wins, losses, totalPlays, regionId);
    }

    private Map<AggregatedStatisticId, AggregatedStatistic> aggregateStatistics(
            Map<String, IndividualStatistic> playerCharacters,
            String category,
            int gameVersion,
            Map<AggregatedStatisticId, AggregatedStatistic> existingStats) {

        Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData = new HashMap<>();
        Map<AggregatedStatisticId, Set<String>> playersPerStat = new HashMap<>();

        for (Map.Entry<String, IndividualStatistic> entry : playerCharacters.entrySet()) {
                String playerId = entry.getKey();
                IndividualStatistic data = entry.getValue();

                AggregatedStatisticId id = createStatisticId(gameVersion, data, category);
                AggregatedStatistic stat = getOrCreateStatistic(id, existingStats, aggregatedData);

                updateStatisticCounts(stat, data);
                playersPerStat.computeIfAbsent(id, k -> new HashSet<>()).add(playerId);
                aggregatedData.put(id, stat);
        }
        updatePlayerCounts(aggregatedData, playersPerStat);
        return aggregatedData;
    }

    private Map<AggregatedStatisticId, AggregatedStatistic> aggregateOverallStatistics(
            Map<String, List<IndividualStatistic>> allPlayerCharacters,
            int gameVersion,
            Map<AggregatedStatisticId, AggregatedStatistic> existingStats) {

        Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData = new HashMap<>();
        Map<AggregatedStatisticId, Set<String>> playersPerStat = new HashMap<>();

        for (Map.Entry<String, List<IndividualStatistic>> entry : allPlayerCharacters.entrySet()) {
            String playerId = entry.getKey();
            List<IndividualStatistic> characterDataList = entry.getValue();

            for (IndividualStatistic data : characterDataList) {
                AggregatedStatisticId id = createStatisticId(gameVersion, data, OVERALL_CATEGORY);
                AggregatedStatistic stat = getOrCreateStatistic(id, existingStats, aggregatedData);

                updateStatisticCounts(stat, data);
                playersPerStat.computeIfAbsent(id, k -> new HashSet<>()).add(playerId);
                aggregatedData.put(id, stat);
            }
        }
        updatePlayerCounts(aggregatedData, playersPerStat);
        return aggregatedData;
    }

    private AggregatedStatisticId createStatisticId(int gameVersion, IndividualStatistic data, String category) {
        return new AggregatedStatisticId(
                gameVersion,
                data.getCharacterId(),
                data.getDanRank(),
                category,
                data.getRegionId()
        );
    }

    private AggregatedStatistic getOrCreateStatistic(
            AggregatedStatisticId id,
            Map<AggregatedStatisticId, AggregatedStatistic> existingStats,
            Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData) {

        AggregatedStatistic stat = existingStats.get(id);

        if (stat == null) {
            stat = new AggregatedStatistic(id);
            stat.setComputedAt(LocalDateTime.now());
        } else if (!aggregatedData.containsKey(id)) {
            // Reset counts if this is the first time we're processing this existing statistic
            resetStatisticCounts(stat);
        }

        return stat;
    }

    private void resetStatisticCounts(AggregatedStatistic stat) {
        stat.setTotalWins(0);
        stat.setTotalLosses(0);
        stat.setTotalPlayers(0);
        stat.setTotalReplays(0);
        stat.setComputedAt(LocalDateTime.now());
    }

    private void updateStatisticCounts(AggregatedStatistic stat, IndividualStatistic data) {
        stat.setTotalWins(stat.getTotalWins() + data.getWins());
        stat.setTotalLosses(stat.getTotalLosses() + data.getLosses());
        stat.setTotalReplays(stat.getTotalReplays() + data.getTotalPlays());
    }

    private void updatePlayerCounts(
            Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData,
            Map<AggregatedStatisticId, Set<String>> playersPerStat) {

        for (Map.Entry<AggregatedStatisticId, AggregatedStatistic> entry : aggregatedData.entrySet()) {
            AggregatedStatisticId id = entry.getKey();
            AggregatedStatistic stat = entry.getValue();
            Set<String> players = playersPerStat.getOrDefault(id, Collections.emptySet());
            stat.setTotalPlayers(players.size());
        }
    }

    private Map<AggregatedStatisticId, AggregatedStatistic> loadExistingStatistics(int gameVersion, String category) {
        List<AggregatedStatistic> existingStats =
                aggregatedStatisticsRepository.findByIdGameVersionAndIdCategory(gameVersion, category);

        return existingStats.stream()
                .collect(Collectors.toMap(AggregatedStatistic::getId, Function.identity()));
    }

    private void saveAggregatedStatistics(Collection<AggregatedStatistic> statistics) {
        aggregatedStatisticsRepository.saveAll(statistics);
    }

    private boolean acquireProcessingLock() {
        return isProcessing.compareAndSet(false, true);
    }

    private void releaseProcessingLock() {
        isProcessing.set(false);
    }
}