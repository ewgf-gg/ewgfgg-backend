package org.ewgf.services;

import org.ewgf.events.ReplayProcessingCompletedEvent;
import org.ewgf.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.ewgf.models.BattleType.RANKED_BATTLE;

@Service
public class BattleProcessingService {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final long COOLDOWN_PERIOD = TimeUnit.MINUTES.toMillis(2); // 2 minute cooldown
    private final AtomicLong lastEventPublishTime = new AtomicLong(0);
    private final AtomicBoolean isPublishing = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(BattleProcessingService.class);

    public BattleProcessingService(JdbcTemplate jdbcTemplate,
                                   ApplicationEventPublisher eventPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processBattlesAsync(List<Battle> battles) {
        // this will drop any duplicate battles from the batch
        Set<String> insertedBattleIds = executeBattleBatchWrite(battles);
        if (insertedBattleIds.isEmpty()) {
            logger.info("No battles inserted, skipping processing");
            return;
        }

        List<Battle> InsertedRankedBattles = battles.stream()
                .filter(battle -> insertedBattleIds.contains(battle.getBattleId()) && battle.getBattleType() == RANKED_BATTLE)
                .toList();

        List<Battle> InsertedUnrankedBattles = battles.stream()
                .filter(battle -> insertedBattleIds.contains(battle.getBattleId()) && battle.getBattleType() != RANKED_BATTLE)
                .toList();

        int unrankedBattleCount = InsertedUnrankedBattles.size();
        if (unrankedBattleCount > 0) updateUnrankedBattleCount(unrankedBattleCount);

        Set<Integer> gameVersionsToProcess = extractGameVersions(InsertedRankedBattles);
        HashMap<String, Player> updatedPlayers = new HashMap<>();

        // Instantiate objects and update relevant information
        processBattlesAndPlayers(InsertedRankedBattles, updatedPlayers);
        executePlayerUpdateOperations(updatedPlayers, insertedBattleIds.size());
        tryPublishEvent(gameVersionsToProcess);
    }

    private void executePlayerUpdateOperations(Map<String, Player> updatedPlayers, Integer insertedBattleCount ) {
        executePlayerBulkOperations(updatedPlayers);
        executeCharacterStatsBulkOperations(updatedPlayers);
        updateRankedBattleCount(insertedBattleCount);
    }

    private void processBattlesAndPlayers(
            List<Battle> battles,
            HashMap<String, Player> updatedPlayers) {
        if(battles.isEmpty()) {
            logger.warn("Battle batch was empty, Skipping player updates.");
            return;
        }
        long startTime = System.currentTimeMillis();


        for (Battle battle : battles) {

                if(battle.getBattleType() != RANKED_BATTLE) continue;

                // Process Player 1
                String player1Id = getPlayerUserIdFromBattle(battle, 1);
                Player player1 = updatedPlayers.get(player1Id);
                if (player1 == null) {
                    player1 = new Player();
                    setPlayerStatsWithBattle(player1, battle, 1);
                    updatedPlayers.put(player1Id, player1);
                }
                setCharacterStatsWithBattle(player1, battle, 1);

                // Process Player 2
                String player2Id = getPlayerUserIdFromBattle(battle, 2);
                Player player2 = updatedPlayers.get(player2Id);
                if (player2 == null) {
                    player2 = new Player();
                    setPlayerStatsWithBattle(player2, battle, 2);
                    updatedPlayers.put(player2Id, player2);
                }
                setCharacterStatsWithBattle(player2, battle, 2);
            }

        logger.info("Updated player and battle information: {} ms", (System.currentTimeMillis() - startTime));
    }


    public Set<String> executeBattleBatchWrite(List<Battle> batch) {
        try {
            int CHUNK_SIZE = 1000;
            long startTime = System.currentTimeMillis();
            // insert battle and increment replay count, else do nothing

            Set<String> inserted = new HashSet<>(batch.size());
            // we keep the whole operation in one transaction so it is still atomic
            jdbcTemplate.execute((Connection con) -> {
                con.setAutoCommit(false);
                return null;
            });

            for (int start = 0; start < batch.size(); start += CHUNK_SIZE) {
                int end = Math.min(start + CHUNK_SIZE, batch.size());
                List<Battle> slice = batch.subList(start, end);

                inserted.addAll(insertChunk(slice));
            }

            logger.info("All Battles inserted successfully: {} ms", (System.currentTimeMillis() - startTime));
            return inserted;
        }
        catch (Exception e) {
            logger.error("BATTLE INSERTION FAILED: {} ", e.getMessage());
            throw e;
        }
    }

    private Set<String> insertChunk(List<Battle> chunk) {
        String SQL =
                "INSERT INTO battles (" +
                        "battle_id, date, battle_at, battle_type, game_version, " +
                        "player1_character_id, player1_name, player1_region, " +
                        "player1_language, player1_polaris_id, player1_tekken_power, player1_dan_rank, " +
                        "player1_rating_before, player1_rating_change, player1_rounds_won, player1_id, " +
                        "player2_character_id, player2_name, player2_region, player2_language, " +
                        "player2_polaris_id, player2_tekken_power, player2_dan_rank, " +
                        "player2_rating_before, player2_rating_change, player2_rounds_won, player2_id, " +
                        "stageid, winner" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (battle_id) DO NOTHING " +
                        "RETURNING battle_id";

        for (Battle battle : chunk) {
            battle.setDate(getReadableDateInUTC(battle));
        }

        return jdbcTemplate.execute(
                (Connection con) -> con.prepareStatement(
                        SQL,
                        new String[]{"battle_id"}
                ),
                (PreparedStatement ps) -> {
                    for (Battle b : chunk) {
                        int i = 1;
                        ps.setString(i++, b.getBattleId());
                        ps.setString(i++, b.getDate());
                        ps.setLong(i++, b.getBattleAt());
                        ps.setInt(i++, b.getBattleType().getBattleCode());
                        ps.setInt(i++, b.getGameVersion());
                        ps.setInt(i++, b.getPlayer1CharacterId());
                        ps.setString(i++, b.getPlayer1Name());
                        setNullableInt(ps, i++, b.getPlayer1RegionId());
                        ps.setString(i++, b.getPlayer1Language());
                        ps.setString(i++, b.getPlayer1PolarisId());
                        ps.setLong(i++, b.getPlayer1TekkenPower());
                        ps.setInt(i++, b.getPlayer1DanRank());
                        setNullableInt(ps, i++, b.getPlayer1RegionId());
                        setNullableInt(ps, i++, b.getPlayer1RatingChange());
                        ps.setInt(i++, b.getPlayer1RoundsWon());
                        ps.setString(i++, b.getPlayer1UserId());
                        ps.setInt(i++, b.getPlayer2CharacterId());
                        ps.setString(i++, b.getPlayer2Name());
                        setNullableInt(ps, i++, b.getPlayer2RegionId());
                        ps.setString(i++, b.getPlayer2Language());
                        ps.setString(i++, b.getPlayer2PolarisId());
                        ps.setLong(i++, b.getPlayer2TekkenPower());
                        ps.setInt(i++, b.getPlayer2DanRank());
                        setNullableInt(ps, i++, b.getPlayer2RatingBefore());
                        setNullableInt(ps, i++, b.getPlayer2RatingChange());
                        ps.setInt(i++, b.getPlayer2RoundsWon());
                        ps.setString(i++, b.getPlayer2UserId());
                        ps.setInt(i++, b.getStageId());
                        ps.setInt(i++, b.getWinner());
                        ps.addBatch();
                    }

                    /* Actually run the batch — one round‑trip */
                    ps.executeBatch();

                    /* Postgres returns one row per **successful** insert */
                    Set<String> ids = new HashSet<>();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        while (rs != null && rs.next()) {
                            ids.add(rs.getString(1));
                        }
                    }
                    return ids; // Spring returns this to the caller
                });
    }
    
    public void executePlayerBulkOperations(Map<String, Player> updatedPlayersMap)
    {

        if (updatedPlayersMap.isEmpty()) {
            logger.debug("Updated Player Set is empty! (Battle batch already existed in database)");
            return;
        }

        long startTime = System.currentTimeMillis();

        String sql =
                "INSERT INTO players " +
                        "(player_id, name, region_id, language, polaris_id, tekken_power, latest_battle) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (player_id) DO UPDATE SET " +

                        "name = CASE WHEN EXCLUDED.latest_battle > players.latest_battle " +
                        "THEN EXCLUDED.name " +
                        "ELSE players.name END, " +

                        "tekken_power = CASE WHEN EXCLUDED.latest_battle > players.latest_battle " +
                        "THEN EXCLUDED.tekken_power " +
                        "ELSE players.tekken_power END, " +

                        "region_id = CASE " +
                        "WHEN EXCLUDED.latest_battle > players.latest_battle THEN COALESCE(EXCLUDED.region_id, players.region_id) " +
                        "ELSE players.region_id END, " +

                        "language = CASE " +
                        "WHEN EXCLUDED.latest_battle > players.latest_battle THEN COALESCE(EXCLUDED.language, players.language) " +
                        "ELSE players.language END, " +

                        "latest_battle = CASE WHEN EXCLUDED.latest_battle > players.latest_battle " +
                        "THEN EXCLUDED.latest_battle " +
                        "ELSE players.latest_battle END ";

        List<Object[]> batchArgs = getPlayerBatchObjects(updatedPlayersMap);

        batchArgs.sort(Comparator.comparing((Object[] args) -> (String) args[0]));

        // Execute batch update
        jdbcTemplate.batchUpdate(sql, batchArgs);

        logger.info("Player Bulk Upsert: {} ms, Processed Players: {}",
                (System.currentTimeMillis() - startTime), updatedPlayersMap.size());
    }

    public void executeCharacterStatsBulkOperations(Map<String, Player> updatedPlayersSet) {
        if (updatedPlayersSet.isEmpty()) {
            logger.debug("Player set is empty, character updates aborted (Battle batch already existed in database)");
            return;
        }

        long startTime = System.currentTimeMillis();

        String sql =
                "INSERT INTO character_stats " +
                        "(player_id, character_id, game_version, dan_rank, latest_battle, wins, losses) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (player_id, character_id, game_version) DO UPDATE SET " +

                        "dan_rank = CASE WHEN EXCLUDED.latest_battle > character_stats.latest_battle " +
                        "THEN EXCLUDED.dan_rank " +
                        "ELSE character_stats.dan_rank END, " +

                        "latest_battle = CASE WHEN EXCLUDED.latest_battle > character_stats.latest_battle " +
                        "THEN EXCLUDED.latest_battle " +
                        "ELSE character_stats.latest_battle END, " +

                        "wins = character_stats.wins + EXCLUDED.wins, " +
                        "losses = character_stats.losses + EXCLUDED.losses";

        List<Object[]> batchArgs = getCharacterStatsBatchObjects(updatedPlayersSet);

        // Sorting to reduce the rate of deadlocks occurring
        batchArgs.sort(Comparator.comparing((Object[] args) -> (String) args[0]) // player_id
                .thenComparing(args -> (String) args[1])  // character_id
                .thenComparing(args -> (Integer) args[2])); // game_version

        jdbcTemplate.batchUpdate(sql, batchArgs);

        logger.info("CharacterStats Bulk Upsert: {} ms, Total Processed CharacterStats: {}",
                (System.currentTimeMillis() - startTime), batchArgs.size());
    }

    private void setCharacterStatsWithBattle(Player player, Battle battle, int playerNumber) {
        String characterId = getPlayerCharacterFromBattle(battle, playerNumber);
        int gameVersion = battle.getGameVersion();

        CharacterStatsId statsId = new CharacterStatsId();
        statsId.setPlayerId(player.getPlayerId());
        statsId.setCharacterId(characterId);
        statsId.setGameVersion(gameVersion);

        // Fetch or initialize CharacterStats for this character and game version
        CharacterStats stats = player.getCharacterStats().get(statsId);

        if (stats == null) {
            stats = new CharacterStats();
            stats.setId(statsId);
            stats.setDanRank(getPlayerDanRankFromBattle(battle, playerNumber));
            player.getCharacterStats().put(statsId, stats);
        }

        // Update wins or losses based on the battle result
        if (battle.getWinner() == playerNumber) {
            stats.setWinsIncrement(stats.getWinsIncrement() + 1);
            stats.setWins(stats.getWins() + 1);
        }
        else {
            stats.setLossIncrement(stats.getLossIncrement() + 1);
            stats.setLosses(stats.getLosses() + 1);
        }

        // Only update the danRank and latest battle if this battle is newer than the current latest one in the batch
        if (battle.getBattleAt() > stats.getLatestBattle()) {
            stats.setLatestBattle(battle.getBattleAt());
            player.setLatestBattle(battle.getBattleAt());
            player.updateTekkenPower(
                    (getPlayerCharacterFromBattle(battle, playerNumber).equals("1") ? battle.getPlayer1TekkenPower() : battle.getPlayer2TekkenPower()),
                    battle.getBattleAt()
            );
            stats.setDanRank(getPlayerDanRankFromBattle(battle, playerNumber));
        }
    }

    private void setPlayerStatsWithBattle(Player player, Battle battle, int playerNumber) {
        player.setPlayerId(getPlayerUserIdFromBattle(battle, playerNumber));
        player.setName(getPlayerNameFromBattle(battle, playerNumber));
        player.setPolarisId(getPlayerPolarisIdFromBattle(battle, playerNumber));
        player.setTekkenPower(getPlayerTekkenPowerFromBattle(battle, playerNumber));
        player.setLanguage(getPlayerLanguageFromBattle(battle, playerNumber));
        player.setRegionId(getPlayerRegionIDFromBattle(battle, playerNumber));

        CharacterStats characterStats = new CharacterStats();
        CharacterStatsId statsId = new CharacterStatsId();

        statsId.setPlayerId(player.getPlayerId());
        statsId.setCharacterId(getPlayerCharacterFromBattle(battle, playerNumber));
        statsId.setGameVersion(battle.getGameVersion());

        characterStats.setId(statsId);
        characterStats.setDanRank(getPlayerDanRankFromBattle(battle, playerNumber));
        characterStats.setLatestBattle(battle.getBattleAt());

        player.getCharacterStats().put(statsId, characterStats);
        player.setLatestBattle(battle.getBattleAt());
    }

    private void tryPublishEvent(Set<Integer> gameVersions)
    {
        long currentTime = System.currentTimeMillis();
        long lastPublishTime = lastEventPublishTime.get();

        // check if enough time has passed since last publish
        if ((currentTime - lastPublishTime) < COOLDOWN_PERIOD) {
            logger.info("Skipping statistics computation due to cooldown");
            return;
        }

        // Try to acquire the publishing lock
        if (!isPublishing.compareAndSet(false, true)) {
            logger.warn("Another thread is currently publishing an event");
            return;
        }

        try {
            // Double-check the time again now that we have the lock
            // This prevents multiple events being published if multiple threads
            // pass the first time check simultaneously
            if ((currentTime - lastEventPublishTime.get()) >= COOLDOWN_PERIOD) {
                eventPublisher.publishEvent(new ReplayProcessingCompletedEvent(gameVersions));
                lastEventPublishTime.set(currentTime);
                logger.debug("Published statistics computation event");
            }
        }
        finally {
            isPublishing.set(false);
        }
    }

    private Set<Integer> extractGameVersions(List<Battle> battles) {
        HashSet<Integer> gameVersions = new HashSet<>();
        for (Battle battle : battles) {
            gameVersions.add(battle.getGameVersion());
        }
        return gameVersions;
    }

    private List<Object[]> getPlayerBatchObjects(Map<String, Player> updatedPlayersMap) {
        List<Object[]> batchArgs = new ArrayList<>();

        for (Player updatedPlayer : updatedPlayersMap.values()) {
            Object[] args = new Object[]{
                    updatedPlayer.getPlayerId(),
                    updatedPlayer.getName(),
                    updatedPlayer.getRegionId(),
                    updatedPlayer.getLanguage(),
                    updatedPlayer.getPolarisId(),
                    updatedPlayer.getTekkenPower(),
                    updatedPlayer.getLatestBattle()
            };
            batchArgs.add(args);
        }
        return batchArgs;
    }

    private List<Object[]> getCharacterStatsBatchObjects(Map<String, Player> updatedPlayersSet) {
        List<Object[]> batchArgs = new ArrayList<>();

        for (Player updatedPlayer : updatedPlayersSet.values()) {
            String userId = updatedPlayer.getPlayerId();

            Map<CharacterStatsId, CharacterStats> updatedCharacterStats = updatedPlayer.getCharacterStats();
            if (updatedCharacterStats != null) {
                for (Map.Entry<CharacterStatsId, CharacterStats> entry : updatedCharacterStats.entrySet()) {
                    CharacterStatsId statsId = entry.getKey();
                    CharacterStats updatedStats = entry.getValue();

                    int winsIncrement = updatedStats.getWinsIncrement();
                    int lossesIncrement = updatedStats.getLossIncrement();

                    Object[] args = new Object[]{
                            userId,
                            statsId.getCharacterId(),
                            statsId.getGameVersion(),
                            updatedStats.getDanRank(),
                            updatedStats.getLatestBattle(),
                            winsIncrement,
                            lossesIncrement
                    };

                    batchArgs.add(args);
                }
            }
        }
        return batchArgs;
    }

    private void updateRankedBattleCount(int battleCount) {
        if (battleCount == 0) return;
        String sql = "UPDATE tekken_stats_summary SET " +
                "total_replays = total_replays + ?";

        jdbcTemplate.update(sql, battleCount);
    }

    private void updateUnrankedBattleCount(int battleCount) {
        if (battleCount == 0) return;
        String sql = "UPDATE tekken_stats_summary SET " +
                "total_unranked_replays = total_unranked_replays + ?";

        jdbcTemplate.update(sql, battleCount);
    }

    private String getReadableDateInUTC(Battle battle) {
        return Instant.ofEpochSecond(battle.getBattleAt())
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss 'UTC'"));
    }

    private String getPlayerNameFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1Name() : battle.getPlayer2Name();
    }

    private String getPlayerLanguageFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1Language() : battle.getPlayer2Language();
    }

    private Integer getPlayerRegionIDFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1RegionId() : battle.getPlayer2RegionId();
    }

    private String getPlayerCharacterFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? String.valueOf(battle.getPlayer1CharacterId()) : String.valueOf(battle.getPlayer2CharacterId());
    }

    private String getPlayerUserIdFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1UserId() : battle.getPlayer2UserId();
    }

    private String getPlayerPolarisIdFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1PolarisId() : battle.getPlayer2PolarisId();
    }

    private long getPlayerTekkenPowerFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1TekkenPower() : battle.getPlayer2TekkenPower();
    }

    private int getPlayerDanRankFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1DanRank() : battle.getPlayer2DanRank();
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer val)
            throws SQLException {
        if (val == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, val);
    }

}


