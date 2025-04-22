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
import org.ewgf.aggregations.PlayerCharacterData;
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

        // main character per player
        processStandardStatistics(allStats, gameVersion);

        // all characters per player
        processOverallStatistics(allStats, gameVersion);
    }

    private void processStandardStatistics(List<Object[]> allStats, int gameVersion) {
        Map<AggregatedStatisticId, AggregatedStatistic> existingStatsMap =
                loadExistingStatistics(gameVersion, STANDARD_CATEGORY);

        Map<String, PlayerCharacterData> playerMainCharacters = identifyPlayerMainCharacters(allStats);

        Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData =
                aggregateStatistics(playerMainCharacters, STANDARD_CATEGORY, gameVersion, existingStatsMap);

        saveAggregatedStatistics(aggregatedData.values());
    }

    private void processOverallStatistics(List<Object[]> allStats, int gameVersion) {
        Map<AggregatedStatisticId, AggregatedStatistic> existingStatsMap =
                loadExistingStatistics(gameVersion, OVERALL_CATEGORY);

        Map<String, List<PlayerCharacterData>> allPlayerCharacters = getAllPlayerCharacters(allStats);

        Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData =
                aggregateOverallStatistics(allPlayerCharacters, gameVersion, existingStatsMap);

        saveAggregatedStatistics(aggregatedData.values());
    }

    private Map<AggregatedStatisticId, AggregatedStatistic> loadExistingStatistics(int gameVersion, String category) {
        List<AggregatedStatistic> existingStats =
                aggregatedStatisticsRepository.findByIdGameVersionAndIdCategory(gameVersion, category);

        return existingStats.stream()
                .collect(Collectors.toMap(AggregatedStatistic::getId, Function.identity()));
    }

    private Map<String, PlayerCharacterData> identifyPlayerMainCharacters(List<Object[]> stats) {
        Map<String, PlayerCharacterData> playerDataMap = new HashMap<>();

        for (Object[] row : stats) {
            // Skip records with missing region or area
            if (row[5] == null || row[6] == null) continue;

            try {
                String playerId = (String) row[0];
                PlayerCharacterData characterData = extractPlayerCharacterData(row);

                // Keep only the character with highest rank or most plays if ranks are equal
                updateMainCharacterIfBetter(playerDataMap, playerId, characterData);
            } catch (ClassCastException | NullPointerException e) {
                logger.error("Error processing row for main character statistics: ", e);
            }
        }

        return playerDataMap;
    }

    private void updateMainCharacterIfBetter(
            Map<String, PlayerCharacterData> playerDataMap,
            String playerId,
            PlayerCharacterData newData) {

        PlayerCharacterData currentData = playerDataMap.get(playerId);

        if (currentData == null ||
                newData.getDanRank() > currentData.getDanRank() ||
                (newData.getDanRank() == currentData.getDanRank() &&
                        newData.getTotalPlays() > currentData.getTotalPlays())) {

            playerDataMap.put(playerId, newData);
        }
    }

    private Map<String, List<PlayerCharacterData>> getAllPlayerCharacters(List<Object[]> stats) {
        Map<String, List<PlayerCharacterData>> playerDataMap = new HashMap<>();

        for (Object[] row : stats) {
            // Skip records with missing region or area
            if (row[5] == null || row[6] == null) continue;

            try {
                String playerId = (String) row[0];
                PlayerCharacterData characterData = extractPlayerCharacterData(row);

                // Add this character to the player's list
                playerDataMap.computeIfAbsent(playerId, k -> new ArrayList<>())
                        .add(characterData);
            } catch (ClassCastException | NullPointerException e) {
                logger.error("Error processing row for overall character statistics: ", e);
            }
        }
        return playerDataMap;
    }

    private PlayerCharacterData extractPlayerCharacterData(Object[] row) {
        String characterId = (String) row[1];
        int danRank = ((Number) row[2]).intValue();
        int wins = ((Number) row[3]).intValue();
        int losses = ((Number) row[4]).intValue();
        int regionId = ((Number) row[5]).intValue();
        int areaId = ((Number) row[6]).intValue();
        int totalPlays = wins + losses;

        return new PlayerCharacterData(characterId, danRank, wins, losses, totalPlays, regionId, areaId);
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
            Map<String, List<PlayerCharacterData>> allPlayerCharacters,
            int gameVersion,
            Map<AggregatedStatisticId, AggregatedStatistic> existingStats) {

        Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData = new HashMap<>();
        Map<AggregatedStatisticId, Set<String>> playersPerStat = new HashMap<>();

        for (Map.Entry<String, List<PlayerCharacterData>> entry : allPlayerCharacters.entrySet()) {
            String playerId = entry.getKey();
            List<PlayerCharacterData> characterDataList = entry.getValue();

            for (PlayerCharacterData data : characterDataList) {
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

    private AggregatedStatisticId createStatisticId(int gameVersion, PlayerCharacterData data, String category) {
        return new AggregatedStatisticId(
                gameVersion,
                data.getCharacterId(),
                data.getDanRank(),
                category,
                data.getRegionID(),
                data.getAreaID()
        );
    }

    private AggregatedStatistic getOrCreateStatistic(
            AggregatedStatisticId id,
            Map<AggregatedStatisticId, AggregatedStatistic> existingStats,
            Map<AggregatedStatisticId, AggregatedStatistic> aggregatedData) {

        AggregatedStatistic stat = existingStats.get(id);

        if (stat == null) {
            // Create new statistic if it doesn't exist
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

    private void updateStatisticCounts(AggregatedStatistic stat, PlayerCharacterData data) {
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