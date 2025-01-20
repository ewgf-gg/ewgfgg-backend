package org.ewgf.services;

import org.ewgf.events.ReplayProcessingCompletedEvent;
import org.ewgf.models.*;
import org.ewgf.repositories.BattleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class BattleProcessingService {

    private final BattleRepository battleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final long COOLDOWN_PERIOD = TimeUnit.MINUTES.toMillis(2); // 2 minute cooldown
    private final AtomicLong lastEventPublishTime = new AtomicLong(0);
    private final AtomicBoolean isPublishing = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(BattleProcessingService.class);

    public BattleProcessingService(BattleRepository battleRepository,
                                   JdbcTemplate jdbcTemplate,
                                   ApplicationEventPublisher eventPublisher)
    {
        this.battleRepository = battleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processBattlesAsync(List<Battle> battles)
    {
        Set<Integer> gameVersionsToProcess = extractGameVersions(battles);

        Map<String, Battle> mapOfExistingBattles = fetchSurroundingBattles(battles);

        HashMap<String, Player> updatedPlayers = new HashMap<>();
        Set<Battle> battleSet = new HashSet<>();

        // Instantiate objects and update relevant information
        processBattlesAndPlayers(battles, mapOfExistingBattles, updatedPlayers, battleSet);
        executeAllDatabaseOperations(updatedPlayers, battleSet);

        tryPublishEvent(gameVersionsToProcess);
    }

    private void executeAllDatabaseOperations(Map<String, Player> updatedPlayers, Set<Battle> battleSet)
    {
        int battleCount = executeBattleBatchWrite(battleSet);
        executePlayerBulkOperations(updatedPlayers);
        executeCharacterStatsBulkOperations(updatedPlayers);
        updateSummaryStatistics(battleCount);
    }

    private Map<String, Battle> fetchSurroundingBattles(List<Battle> battles)
    {
        if (battles.isEmpty())
        {
            logger.warn("No battles provided. Skipping database fetch.");
            return Collections.emptyMap();
        }

        long startTime = System.currentTimeMillis();

        // fetch the timestamp of the median battle in the list
        long middleTimestamp = battles.get(battles.size() / 2).getBattleAt();

        // Fetch surrounding battle IDs directly as a Set
        Set<String> surroundingBattleIdSet = new HashSet<>(battleRepository.findSurroundingBattleIds(middleTimestamp));

        // Create a map of existing battles
        Map<String, Battle> existingBattles = battles.stream()
                .filter(battle -> surroundingBattleIdSet.contains(battle.getBattleId()))
                .collect(Collectors.toMap(Battle::getBattleId, battle -> battle));

        long endTime = System.currentTimeMillis();
        logger.info("Identified {} existing battles in {} ms", existingBattles.size(), (endTime - startTime));

        return existingBattles;
    }


    private void processBattlesAndPlayers(
            List<Battle> battles,
            Map<String, Battle> existingBattlesMap,
            HashMap<String, Player> updatedPlayers,
            Set<Battle> battleSet)
    {

        long startTime = System.currentTimeMillis();
        int duplicateBattles = 0;

        for (Battle battle : battles)
        {
            if (!existingBattlesMap.containsKey(battle.getBattleId()))
            {
                battle.setDate(getReadableDateInUTC(battle));

                // Process Player 1
                String player1Id = getPlayerUserIdFromBattle(battle, 1);
                Player player1 = updatedPlayers.get(player1Id);
                if (player1 == null)
                {
                    player1 = new Player();
                    setPlayerStatsWithBattle(player1, battle, 1);
                    updatedPlayers.put(player1Id, player1);
                }
                setCharacterStatsWithBattle(player1, battle, 1);

                // Process Player 2
                String player2Id = getPlayerUserIdFromBattle(battle, 2);
                Player player2 = updatedPlayers.get(player2Id);
                if (player2 == null)
                {
                    player2 = new Player();
                    setPlayerStatsWithBattle(player2, battle, 2);
                    updatedPlayers.put(player2Id, player2);
                }
                setCharacterStatsWithBattle(player2, battle, 2);
                battleSet.add(battle);
            } else
            {
                duplicateBattles++;
            }
        }
        long endTime = System.currentTimeMillis();
        if (duplicateBattles == battles.size()) {
            logger.warn("Entire batch already exists in database!");
            return;
        }
        logger.info("Updated player and battle information: {} ms", (endTime - startTime));
    }


    public int executeBattleBatchWrite(Set<Battle> battleSet)
    {
        if (battleSet == null || battleSet.isEmpty()) {
            logger.warn("No battles to insert or update.");
            return 0;
        }

        try {
            long startTime = System.currentTimeMillis();

            // insert battle and increment replay count, else do nothing
            String sql =
                    "INSERT INTO battles (" +
                            "battle_id, date, battle_at, battle_type, game_version, " +
                            "player1_character_id, player1_name, player1_region, player1_area, " +
                            "player1_language, player1_polaris_id, player1_tekken_power, player1_dan_rank, " +
                            "player1_rating_before, player1_rating_change, player1_rounds_won, player1_id, " +
                            "player2_character_id, player2_name, player2_region, player2_area, player2_language, " +
                            "player2_polaris_id, player2_tekken_power, player2_dan_rank, " +
                            "player2_rating_before, player2_rating_change, player2_rounds_won, player2_id, " +
                            "stageid, winner" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT (battle_id) DO NOTHING";

            List<Object[]> batchArgs = getBattleBatchObjects(battleSet);

            int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
            long endTime = System.currentTimeMillis();

            int insertedCount = Arrays.stream(results).sum();

            logger.info("Battle Insertion: {} ms, Inserted Count: {}", (endTime - startTime), insertedCount);

            return insertedCount;
        }
        catch (Exception e)
        {
            logger.error("BATTLE INSERTION FAILED: ", e);
            return 0;
        }
    }
    
    public void executePlayerBulkOperations(Map<String, Player> updatedPlayersMap)
    {

        if (updatedPlayersMap.isEmpty()) {
            logger.warn("Updated Player Set is empty! (Battle batch already existed in database)");
            return;
        }

        long startTime = System.currentTimeMillis();

        String sql =
                "INSERT INTO players " +
                        "(player_id, name, region_id, area_id, language, polaris_id, tekken_power, latest_battle) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (player_id) DO UPDATE SET " +

                        "name = CASE WHEN EXCLUDED.latest_battle > players.latest_battle " +
                        "THEN EXCLUDED.name " +
                        "ELSE players.name END, " +

                        "tekken_power = CASE WHEN EXCLUDED.latest_battle > players.latest_battle " +
                        "THEN EXCLUDED.tekken_power " +
                        "ELSE players.tekken_power END, " +

                        "region_id = CASE " +
                        "WHEN players.region_id IS NULL AND EXCLUDED.region_id IS NOT NULL THEN EXCLUDED.region_id " +
                        "WHEN EXCLUDED.latest_battle > players.latest_battle THEN EXCLUDED.region_id " +
                        "ELSE players.region_id END, " +

                        "area_id = CASE " +
                        "WHEN players.area_id IS NULL AND EXCLUDED.area_id IS NOT NULL THEN EXCLUDED.area_id " +
                        "WHEN EXCLUDED.latest_battle > players.latest_battle THEN EXCLUDED.area_id " +
                        "ELSE players.area_id END, " +

                        "language = CASE " +
                        "WHEN players.language IS NULL AND EXCLUDED.language IS NOT NULL THEN EXCLUDED.language " +
                        "WHEN EXCLUDED.latest_battle > players.latest_battle THEN EXCLUDED.language " +
                        "ELSE players.language END, " +

                        "latest_battle = CASE WHEN EXCLUDED.latest_battle > players.latest_battle " +
                        "THEN EXCLUDED.latest_battle " +
                        "ELSE players.latest_battle END ";

        List<Object[]> batchArgs = getPlayerBatchObjects(updatedPlayersMap);

        batchArgs.sort(Comparator.comparing((Object[] args) -> (String) args[0]));

        // Execute batch update
        jdbcTemplate.batchUpdate(sql, batchArgs);
        long endTime = System.currentTimeMillis();

        logger.info("Player Bulk Upsert: {} ms, Processed Players: {}",
                (endTime - startTime), updatedPlayersMap.size());
    }

    public void executeCharacterStatsBulkOperations(Map<String, Player> updatedPlayersSet)
    {
        if (updatedPlayersSet.isEmpty())
        {
            logger.warn("Player set is empty, character updates aborted (Battle batch already existed in database)");
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

        long endTime = System.currentTimeMillis();
        logger.info("CharacterStats Bulk Upsert: {} ms, Total Processed CharacterStats: {}",
                (endTime - startTime), batchArgs.size());
    }

    private void setCharacterStatsWithBattle(Player player, Battle battle, int playerNumber)
    {
        String characterId = getPlayerCharacterFromBattle(battle, playerNumber);
        int gameVersion = battle.getGameVersion();

        // Create a CharacterStatsId for lookup
        CharacterStatsId statsId = new CharacterStatsId();
        statsId.setPlayerId(player.getPlayerId());
        statsId.setCharacterId(characterId);
        statsId.setGameVersion(gameVersion);

        // Fetch or initialize CharacterStats for this character and game version
        CharacterStats stats = player.getCharacterStats().get(statsId);

        if (stats == null)
        {
            // Initialize a new CharacterStats object if none exists
            stats = new CharacterStats();
            stats.setId(statsId);
            stats.setDanRank(getPlayerDanRankFromBattle(battle, playerNumber));
            player.getCharacterStats().put(statsId, stats);
        }

        // Update wins or losses based on the battle result
        if (battle.getWinner() == playerNumber)
        {
            stats.setWinsIncrement(stats.getWinsIncrement() + 1);
            stats.setWins(stats.getWins() + 1);
        }
        else
        {
            stats.setLossIncrement(stats.getLossIncrement() + 1);
            stats.setLosses(stats.getLosses() + 1);
        }

        // Only update the danRank and latest battle if this battle is newer than the current latest one in the batch
        if (battle.getBattleAt() > stats.getLatestBattle())
        {
            stats.setLatestBattle(battle.getBattleAt());
            player.setLatestBattle(battle.getBattleAt());
            player.updateTekkenPower(
                    (getPlayerCharacterFromBattle(battle, playerNumber).equals("1") ? battle.getPlayer1TekkenPower() : battle.getPlayer2TekkenPower()),
                    battle.getBattleAt()
            );
            stats.setDanRank(getPlayerDanRankFromBattle(battle, playerNumber));
        }
    }

    private void updateSummaryStatistics(int newBattleCount)
    {
        if (newBattleCount == 0)
        {
            return;
        }
        String sql = "UPDATE tekken_stats_summary SET " +
                "total_replays = total_replays + ?";

        jdbcTemplate.update(sql, newBattleCount);
    }


    private void setPlayerStatsWithBattle(Player player, Battle battle, int playerNumber)
    {
        // Set player-level details
        player.setPlayerId(getPlayerUserIdFromBattle(battle, playerNumber));
        player.setName(getPlayerNameFromBattle(battle, playerNumber));
        player.setPolarisId(getPlayerPolarisIdFromBattle(battle, playerNumber));
        player.setTekkenPower(getPlayerTekkenPowerFromBattle(battle, playerNumber));
        player.setLanguage(getPlayerLanguageFromBattle(battle, playerNumber));
        player.setRegionId(getPlayerRegionIDFromBattle(battle, playerNumber));
        player.setAreaId(getPlayerAreaIDFromBattle(battle, playerNumber));

        // Create a new CharacterStats object for the player's character
        CharacterStats characterStats = new CharacterStats();

        // Create and set the composite id CharacterStatsId
        CharacterStatsId statsId = new CharacterStatsId();
        statsId.setPlayerId(player.getPlayerId());
        statsId.setCharacterId(getPlayerCharacterFromBattle(battle, playerNumber));
        statsId.setGameVersion(battle.getGameVersion());
        characterStats.setId(statsId);

        characterStats.setDanRank(getPlayerDanRankFromBattle(battle, playerNumber));
        characterStats.setLatestBattle(battle.getBattleAt());

        // Add the new character stats to the player's map
        player.getCharacterStats().put(statsId, characterStats);
        player.setLatestBattle(battle.getBattleAt());

        // Add the player's name to the name history if it's new
        addPlayerNameIfNew(player, player.getName());
    }

    private void tryPublishEvent(Set<Integer> gameVersions)
    {
        long currentTime = System.currentTimeMillis();
        long lastPublishTime = lastEventPublishTime.get();

        // check if enough time has passed since last publish
        if ((currentTime - lastPublishTime) < COOLDOWN_PERIOD)
        {
            logger.info("Skipping statistics computation due to cooldown period");
            return;
        }

        // Try to acquire the publishing lock
        if (!isPublishing.compareAndSet(false, true))
        {
            logger.info("Another thread is currently publishing an event");
            return;
        }

        try
        {
            // Double-check the time again now that we have the lock
            // This prevents multiple events being published if multiple threads
            // pass the first time check simultaneously
            if ((currentTime - lastEventPublishTime.get()) >= COOLDOWN_PERIOD)
            {
                eventPublisher.publishEvent(new ReplayProcessingCompletedEvent(gameVersions));
                lastEventPublishTime.set(currentTime);
                logger.info("Published statistics computation event");
            }
        }
        finally
        {
            isPublishing.set(false);
        }
    }

    private Set<Integer> extractGameVersions(List<Battle> battles)
    {
        HashSet<Integer> gameVersions = new HashSet<>();
        for (Battle battle : battles)
        {
            gameVersions.add(battle.getGameVersion());
        }
        return gameVersions;
    }

    private void addPlayerNameIfNew(Player player, String name)
    {
        if (player.getPlayerNames().stream().noneMatch(pastName -> pastName.getName().equals(name)))
        {
            PastPlayerNames pastName = new PastPlayerNames(name, player);
            player.getPlayerNames().add(pastName);
        }
    }


    private List<Object[]> getBattleBatchObjects(Set<Battle> battleSet)
    {
        List<Object[]> batchArgs = new ArrayList<>();

        // the order of these parameters must match the SQL statement above
        for (Battle battle : battleSet) {
            Object[] args = new Object[] {
                    battle.getBattleId(),
                    battle.getDate(),
                    battle.getBattleAt(),
                    battle.getBattleType(),
                    battle.getGameVersion(),
                    battle.getPlayer1CharacterId(),
                    battle.getPlayer1Name(),
                    battle.getPlayer1RegionId(),
                    battle.getPlayer1AreaId(),
                    battle.getPlayer1Language(),
                    battle.getPlayer1PolarisId(),
                    battle.getPlayer1TekkenPower(),
                    battle.getPlayer1DanRank(),
                    battle.getPlayer1RatingBefore(),
                    battle.getPlayer1RatingChange(),
                    battle.getPlayer1RoundsWon(),
                    battle.getPlayer1UserId(),
                    battle.getPlayer2CharacterId(),
                    battle.getPlayer2Name(),
                    battle.getPlayer2RegionId(),
                    battle.getPlayer2AreaId(),
                    battle.getPlayer2Language(),
                    battle.getPlayer2PolarisId(),
                    battle.getPlayer2TekkenPower(),
                    battle.getPlayer2DanRank(),
                    battle.getPlayer2RatingBefore(),
                    battle.getPlayer2RatingChange(),
                    battle.getPlayer2RoundsWon(),
                    battle.getPlayer2UserId(),
                    battle.getStageId(),
                    battle.getWinner()
            };
            batchArgs.add(args);
        }
        return batchArgs;
    }


    private List<Object[]> getPlayerBatchObjects(Map<String, Player> updatedPlayersMap)
    {
        List<Object[]> batchArgs = new ArrayList<>();

        for (Player updatedPlayer : updatedPlayersMap.values())
        {

            Object[] args = new Object[]{
                    updatedPlayer.getPlayerId(),
                    updatedPlayer.getName(),
                    updatedPlayer.getRegionId(),
                    updatedPlayer.getAreaId(),
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

        for (Player updatedPlayer : updatedPlayersSet.values())
        {
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

    private Integer getPlayerAreaIDFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1AreaId() : battle.getPlayer2AreaId();
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
        return playerNumber == 1 ? battle.getPlayer1PolarisId( ) : battle.getPlayer2PolarisId();
    }

    private long getPlayerTekkenPowerFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1TekkenPower() : battle.getPlayer2TekkenPower();
    }

    private int getPlayerDanRankFromBattle(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1DanRank() : battle.getPlayer2DanRank();
    }

    private int calculatePlayerRating(Battle battle, int playerNumber) {
        if (playerNumber == 1) {
            return (battle.getPlayer1RatingBefore() != null ? battle.getPlayer1RatingBefore() : 0) +
                    (battle.getPlayer1RatingChange() != null ? battle.getPlayer1RatingChange() : 0);
        } else {
            return (battle.getPlayer2RatingBefore() != null ? battle.getPlayer2RatingBefore() : 0) +
                    (battle.getPlayer2RatingChange() != null ? battle.getPlayer2RatingChange() : 0);
        }
    }
}

