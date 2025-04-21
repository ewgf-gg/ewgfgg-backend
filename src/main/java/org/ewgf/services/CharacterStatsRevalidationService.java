package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;
import org.ewgf.configuration.BackpressureManager;
import org.ewgf.models.Battle;
import org.ewgf.models.CharacterStats;
import org.ewgf.models.CharacterStatsId;
import org.ewgf.models.Player;
import org.ewgf.repositories.BattleRepository;
import org.ewgf.repositories.CharacterStatsRepository;
import org.ewgf.repositories.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CharacterStatsRevalidationService {
    private final PlayerRepository playerRepository;
    private final BattleRepository battleRepository;
    private final BackpressureManager backpressureManager;
    private final ExecutorService executorService;
    private volatile boolean isRevalidating = false;
    private final BatchExecutorService batchExecutorService;
    public CharacterStatsRevalidationService(
            PlayerRepository playerRepository,
            BattleRepository battleRepository,
            CharacterStatsRepository characterStatsRepository,
            BackpressureManager backpressureManager,
            BatchExecutorService batchExecutorService) {
        this.playerRepository = playerRepository;
        this.battleRepository = battleRepository;
        this.backpressureManager = backpressureManager;
        this.batchExecutorService = batchExecutorService;
        // Create thread executor using virtual threads
        this.executorService = Executors.newFixedThreadPool(
                25,
                Thread.ofVirtual()
                        .name("revalidation-", 0)
                        .factory()
        );
    }


    public void startRevalidation() {
        if (isRevalidating) {
            log.warn("Revalidation is already in progress");
            return;
        }

        try {
            isRevalidating = true;
            backpressureManager.manualBackpressureActivation(); // Pause RabbitMQ consumption
            log.info("Starting character stats revalidation");

            int pageSize = 1000;
            Page<Player> firstPage = playerRepository.findAll(PageRequest.of(0, pageSize));
            int totalPages = firstPage.getTotalPages();
            long totalPlayers = firstPage.getTotalElements();

            log.info("Starting revalidation of {} players across {} pages", totalPlayers, totalPages);

            AtomicInteger completedPages = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(totalPages);

            // Process first page
            executorService.execute(() -> {
                try {
                    processPlayerPage(firstPage.getContent());
                } finally {
                    int completed = completedPages.incrementAndGet();
                    if (completed % 10 == 0) { // Log every 10 pages
                        log.info("Progress: {} of {} pages completed ({}%)",
                                completed, totalPages,
                                String.format("%.2f", (completed * 100.0) / totalPages));
                    }
                    latch.countDown();
                }
            });

            // Process remaining pages
            for (int i = 1; i < totalPages; i++) {
                int pageNumber = i;
                executorService.execute(() -> {
                    try {
                        Pageable pageable = PageRequest.of(pageNumber, pageSize);
                        Page<Player> page = playerRepository.findAll(pageable);
                        processPlayerPage(page.getContent());
                    } finally {
                        int completed = completedPages.incrementAndGet();
                        if (completed % 10 == 0) { // Log every 10 pages
                            log.info("Progress: {} of {} pages completed ({}%)",
                                    completed, totalPages,
                                    String.format("%.2f", (completed * 100.0) / totalPages));
                        }
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            if (!latch.await(10, TimeUnit.HOURS)) {
                log.error("Revalidation timed out after 10 hour");
                throw new TimeoutException("Revalidation timed out");
            }

        } catch (Exception e) {
            log.error("Error during character stats revalidation", e);
            throw new RuntimeException("Character stats revalidation failed", e);
        } finally {
            isRevalidating = false;
            backpressureManager.manualBackpressureDeactivation(); // Resume RabbitMQ consumption
            log.info("Character stats revalidation completed");
        }
    }

    private void processPlayerPage(List<Player> players) {
        List<Object[]> batchUpdates = new ArrayList<>();

        for (Player player : players) {
            try {
                Optional<List<Battle>> battles = battleRepository.findAllBattlesByPlayerId(player.getPlayerId());
                if (battles.isEmpty()) {
                    continue;
                }

                Map<CharacterStatsId, CharacterStatsAccumulator> statsMap = calculatePlayerStats(battles.get(), player);
                batchUpdates.addAll(prepareBatchUpdates(player.getPlayerId(), statsMap));


            } catch (Exception e) {
                log.error("Error processing player {}: {}", player.getPlayerId(), e.getMessage());
            }
        }

        if (!batchUpdates.isEmpty()) {
            batchExecutorService.executeBatchUpdate(batchUpdates);
        }
    }

    private Map<CharacterStatsId, CharacterStatsAccumulator> calculatePlayerStats
            (List<Battle> battles,
            Player player)
    {
        Map<CharacterStatsId, CharacterStatsAccumulator> statAccumulatorMap = new HashMap<>();

        for (Battle battle : battles) {
            processBattle(battle, player.getPlayerId(), statAccumulatorMap);
        }

        return statAccumulatorMap;
    }

    private void processBattle(
            Battle battle,
            String playerId,
            Map<CharacterStatsId, CharacterStatsAccumulator> statsAccumulatorMap)
    {
        boolean isPlayer1 = playerId.equals(battle.getPlayer1UserId());
        int gameVersion = battle.getGameVersion();
        int characterId = isPlayer1 ? battle.getPlayer1CharacterId() : battle.getPlayer2CharacterId();

        boolean isWinner = battle.getWinner() == (isPlayer1 ? 1 : 2);
        CharacterStatsId statsId = new CharacterStatsId(playerId, String.valueOf(characterId), gameVersion);


        statsAccumulatorMap.computeIfAbsent(statsId, k -> new CharacterStatsAccumulator())
                .update(isWinner);
    }

    private List<Object[]> prepareBatchUpdates(
            String playerId,
            Map<CharacterStatsId, CharacterStatsAccumulator> statsMap) {
        return statsMap.entrySet().stream()
                .map(entry -> new Object[]{
                        playerId,
                        entry.getKey().getCharacterId(),
                        0,
                        entry.getKey().getGameVersion(),
                        entry.getValue().getWins(),
                        entry.getValue().getLosses()
                })
                .collect(Collectors.toList());
    }

    private static class CharacterStatsAccumulator {
        private int wins = 0;
        private int losses = 0;

        void update(boolean isWin) {
            if (isWin) {
                wins++;
            } else {
                losses++;
            }
        }

        int getWins() { return wins; }
        int getLosses() { return losses; }
    }
}