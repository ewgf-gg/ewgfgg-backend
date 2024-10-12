package org.tekkenstats.services;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import org.tekkenstats.models.Battle;
import org.tekkenstats.models.CharacterStats;
import org.tekkenstats.models.PastPlayerNames;
import org.tekkenstats.models.Player;
import org.tekkenstats.configuration.RabbitMQConfig;

import org.tekkenstats.repositories.BattleRepository;
import org.tekkenstats.rowmappers.BattleRowMapper;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RabbitService {

    private static final Logger logger = LogManager.getLogger(RabbitService.class);
    private final BattleRowMapper battleRowMapper = new BattleRowMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BattleRepository battleRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME, containerFactory = "rabbitListenerContainerFactory", concurrency = "6")
    public void receiveMessage(String message, @Header("unixTimestamp") String dateAndTime) throws Exception
    {
        String threadName = Thread.currentThread().getName();
        logger.info("{} Received Battle Data from RabbitMQ, Timestamped: {}", threadName, dateAndTime);

        long startTime = System.currentTimeMillis();

        List<Battle> battles = battleRowMapper.mapToBattlesFromApiJson(message);
        processBattlesAsync(battles);

        long endTime = System.currentTimeMillis();

        logger.info("{} Total Operation Time: {} ms", threadName, endTime - startTime);
    }

    public void processBattlesAsync(List<Battle> battles)
    {
        // Fetch existing battles
        Map<String, Battle> mapOfExistingBattles = fetchExistingBattles(battles);

        HashMap<String, Player> updatedPlayers = new HashMap<>();
        Set<Battle> battleSet = new HashSet<>();

        // Instantiate objects and update relevant information
        processBattlesAndPlayers(battles, mapOfExistingBattles, updatedPlayers, battleSet);

        // Execute player batched writes
        executePlayerBulkOperations(updatedPlayers);
        executeCharacterStatsBulkOperations(updatedPlayers);

        // Execute battle batched writes
        executeBattleBatchWrite(battleSet);
    }

    private void extractBattleIDs(List<Battle> battles, Set<String> battleIDs)
    {
        for (Battle battle : battles)
        {
            battleIDs.add(battle.getBattleId());
        }
    }


    private Map<String, Battle> fetchExistingBattles(List<Battle> battles)
    {
        if (battles.isEmpty())
        {
            logger.warn("No battles provided. Skipping database fetch.");
            return Collections.emptyMap();
        }

        long startTime = System.currentTimeMillis();

        // Get the timestamp of the middle battle in the list
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
                String player1Id = getPlayerUserId(battle, 1);
                Player player1 = updatedPlayers.get(player1Id);
                if (player1 == null)
                {
                    player1 = new Player();
                    updateNewPlayerDetails(player1, battle, 1);
                    updatedPlayers.put(player1Id, player1);
                }
                updatePlayerWithBattle(player1, battle, 1);

                // Process Player 2
                String player2Id = getPlayerUserId(battle, 2);
                Player player2 = updatedPlayers.get(player2Id);
                if (player2 == null)
                {
                    player2 = new Player();
                    updateNewPlayerDetails(player2, battle, 2);
                    updatedPlayers.put(player2Id, player2);
                }
                updatePlayerWithBattle(player2, battle, 2);
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

    public void executeBattleBatchWrite(Set<Battle> battleSet)
    {
        if (battleSet == null || battleSet.isEmpty()) {
            logger.warn("No battles to insert or update.");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();

            String sql = "INSERT INTO battles (" +
                    "battle_id, date, battle_at, battle_type, game_version, " +
                    "player1_character_id, player1_name, player1_polaris_id, player1_tekken_power, player1_dan_rank, " +
                    "player1_rating_before, player1_rating_change, player1_rounds_won, player1_id, " +
                    "player2_character_id, player2_name, player2_polaris_id, player2_tekken_power, player2_dan_rank, " +
                    "player2_rating_before, player2_rating_change, player2_rounds_won, player2_id, " +
                    "stageid, winner" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (battle_id) DO NOTHING";

            List<Object[]> batchArgs = new ArrayList<>();

            for (Battle battle : battleSet) {
                Object[] args = new Object[] {
                        battle.getBattleId(),
                        battle.getDate(),
                        battle.getBattleAt(),
                        battle.getBattleType(),
                        battle.getGameVersion(),
                        battle.getPlayer1CharacterID(),
                        battle.getPlayer1Name(),
                        battle.getPlayer1PolarisID(),
                        battle.getPlayer1TekkenPower(),
                        battle.getPlayer1DanRank(),
                        battle.getPlayer1RatingBefore(),
                        battle.getPlayer1RatingChange(),
                        battle.getPlayer1RoundsWon(),
                        battle.getPlayer1UserID(),
                        battle.getPlayer2CharacterID(),
                        battle.getPlayer2Name(),
                        battle.getPlayer2PolarisID(),
                        battle.getPlayer2TekkenPower(),
                        battle.getPlayer2DanRank(),
                        battle.getPlayer2RatingBefore(),
                        battle.getPlayer2RatingChange(),
                        battle.getPlayer2RoundsWon(),
                        battle.getPlayer2UserID(),
                        battle.getStageID(),
                        battle.getWinner()
                };
                batchArgs.add(args);
            }

            jdbcTemplate.batchUpdate(sql, batchArgs);

            long endTime = System.currentTimeMillis();
            logger.info("Battle Insertion: {} ms, Inserted/Updated: {}", (endTime - startTime), battleSet.size());

        } catch (Exception e) {
            logger.error("BATTLE INSERTION FAILED: ", e);
        }
    }

    public void executePlayerBulkOperations(HashMap<String, Player> updatedPlayersMap)
    {

        if (updatedPlayersMap.isEmpty()) {
            logger.warn("Updated Player Set is empty! (Battle batch already existed in database)");
            return;
        }

        long startTime = System.currentTimeMillis();

        String sql =
                "INSERT INTO players (player_id, name, polaris_id, tekken_power,latest_battle) " +
                "VALUES (?, ?, ?, ? ,?) " +
                "ON CONFLICT (player_id) DO UPDATE SET " +
                "tekken_power = CASE WHEN EXCLUDED.latest_battle > players.latest_battle THEN EXCLUDED.tekken_power ELSE players.tekken_power END," +
                "latest_battle = CASE WHEN EXCLUDED.latest_battle > players.latest_battle THEN EXCLUDED.latest_battle ELSE players.latest_battle END";

        List<Object[]> batchArgs = new ArrayList<>();

        for (Player updatedPlayer : updatedPlayersMap.values())
        {

            Object[] args = new Object[]{
                    updatedPlayer.getPlayerId(),
                    updatedPlayer.getName(),
                    updatedPlayer.getPolarisId(),
                    updatedPlayer.getTekkenPower(),
                    updatedPlayer.getLatestBattle()
            };

            batchArgs.add(args);

        }

        batchArgs.sort(Comparator.comparing((Object[] args) -> (String) args[0]));
        // Execute batch update
        jdbcTemplate.batchUpdate(sql, batchArgs);

        long endTime = System.currentTimeMillis();
        logger.info("Player Bulk Upsert: {} ms, Processed Players: {}",
                (endTime - startTime), updatedPlayersMap.size());
    }


    public void executeCharacterStatsBulkOperations(
            HashMap<String,Player> updatedPlayersSet) {

        if (updatedPlayersSet.isEmpty()) {
            logger.warn("Updated Player Set is empty! (Battle batch already existed in database)");
            return;
        }

        long startTime = System.currentTimeMillis();

        String sql =
                "INSERT INTO character_stats (player_id, character_id, dan_rank, latest_battle, wins, losses) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (player_id, character_id) DO UPDATE SET " +
                "dan_rank = CASE WHEN EXCLUDED.latest_battle > character_stats.latest_battle THEN EXCLUDED.dan_rank ELSE character_stats.dan_rank END, " +
                "latest_battle = CASE WHEN EXCLUDED.latest_battle > character_stats.latest_battle THEN EXCLUDED.latest_battle ELSE character_stats.latest_battle END, " +
                "wins = character_stats.wins + EXCLUDED.wins, " +
                "losses = character_stats.losses + EXCLUDED.losses";

        List<Object[]> batchArgs = new ArrayList<>();

        for (Player updatedPlayer : updatedPlayersSet.values()) {
            String userId = updatedPlayer.getPlayerId();

            Map<String, CharacterStats> updatedCharacterStats = updatedPlayer.getCharacterStats();
            if (updatedCharacterStats != null) {
                for (Map.Entry<String, CharacterStats> entry : updatedCharacterStats.entrySet()) {
                    String characterName = entry.getKey();
                    CharacterStats updatedStats = entry.getValue();

                    int winsIncrement = updatedStats.getWinsIncrement();
                    int lossesIncrement = updatedStats.getLossIncrement();

                    Object[] args = new Object[]{
                            userId,
                            characterName,
                            updatedStats.getDanRank(),
                            updatedStats.getLatestBattle(),
                            winsIncrement,
                            lossesIncrement
                    };

                    batchArgs.add(args);

                }
            }
        }
        // Sorting to reduce the rate of deadlocks occurring
        batchArgs.sort(Comparator.comparing((Object[] args) -> (String) args[0]) // player_id
                .thenComparing(args -> (String) args[1]));     // character_id

        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;

        while (!success && retryCount <= maxRetries) {
            try
            {
                int batchSize = 1000; // Adjust based on your system's capacity
                int totalBatches = (int) Math.ceil((double) batchArgs.size() / batchSize);

                for (int i = 0; i < totalBatches; i++)
                {
                    int start = i * batchSize;
                    int end = Math.min(start + batchSize, batchArgs.size());

                    List<Object[]> batch = batchArgs.subList(start, end);

                    jdbcTemplate.batchUpdate(sql, batch);
                }
                success = true; // If execution reaches here, the operation succeeded
            }
            catch (DataAccessException e)
            {
                Throwable rootCause = e.getRootCause();
                if (rootCause instanceof SQLException)
                {
                    SQLException sqlEx = (SQLException) rootCause;
                    if ("40P01".equals(sqlEx.getSQLState()))
                    {
                        // **Deadlock detected, retrying**
                        retryCount++;
                        logger.warn("Deadlock detected during character_stats batch update. Retrying... attempt {}/{}", retryCount, maxRetries);
                        try
                        {
                            // Exponential backoff
                            Thread.sleep((long) Math.pow(2, retryCount) * 100);
                        } catch (InterruptedException ie)
                        {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Thread interrupted during retry sleep", ie);
                        }
                    } else
                    {
                        // Other SQL exception
                        throw e;
                    }
                } else
                {
                    // Non-SQL exception
                    throw e;
                }
            }
        }

        if (!success)
        {
            throw new RuntimeException("Failed to execute character_stats batch update after " + maxRetries + " attempts due to deadlocks.");
        }

        long endTime = System.currentTimeMillis();
        logger.info("CharacterStats Bulk Upsert: {} ms, Total Processed CharacterStats: {}",
                (endTime - startTime), batchArgs.size());
    }

    private void updatePlayerWithBattle(Player player, Battle battle, int playerNumber) {
        // Get the character ID associated with the player for this battle
        String characterId = getPlayerCharacter(battle, playerNumber);

        // Fetch or initialize CharacterStats for this character
        CharacterStats stats = player.getCharacterStats().get(characterId);

        if (stats == null)
        {
            // Initialize a new CharacterStats object if none exists for the character
            stats = new CharacterStats();
            stats.setDanRank(getPlayerDanRank(battle, playerNumber));
            player.getCharacterStats().put(characterId, stats);
        }

        // Update wins or losses based on the battle result
        if (battle.getWinner() == playerNumber)
        {
            stats.setWinsIncrement(stats.getWinsIncrement() + 1);
            stats.setWins(stats.getWins() + 1);
        } else
        {
            stats.setLossIncrement(stats.getLossIncrement() + 1);
            stats.setLosses(stats.getLosses() + 1);
        }

        // Only update the danRank and latest battle if this battle is newer than the current latest one
        if (battle.getBattleAt() > stats.getLatestBattle())
        {
            stats.setLatestBattle(battle.getBattleAt());
            player.setLatestBattle();
            player.updateTekkenPower((getPlayerCharacter(battle, playerNumber).equals("1") ? battle.getPlayer1TekkenPower() : battle.getPlayer2TekkenPower()), battle.getBattleAt());
            stats.setDanRank(getPlayerDanRank(battle, playerNumber));  // Update dan rank only if the battle is the latest
        }
    }


    private void updateNewPlayerDetails(Player player, Battle battle, int playerNumber) {
        // Set player-level details
        player.setPlayerId(getPlayerUserId(battle, playerNumber));
        player.setName(getPlayerName(battle, playerNumber));
        player.setPolarisId(getPlayerPolarisId(battle, playerNumber));
        player.setTekkenPower(getPlayerTekkenPower(battle, playerNumber));

        // Create a new CharacterStats object for the player's character
        CharacterStats characterStats = new CharacterStats();
        characterStats.setDanRank(getPlayerDanRank(battle, playerNumber));
        characterStats.setLatestBattle(battle.getBattleAt());

        // Add the new character stats to the player's map, keyed by character ID
        String characterId = getPlayerCharacter(battle, playerNumber);
        player.getCharacterStats().put(characterId, characterStats);
        player.setLatestBattle();

        // Add the player's name to the name history if it's new
        addPlayerNameIfNew(player, player.getName());
    }


    private void addPlayerNameIfNew(Player player, String name)
    {
        if (player.getPlayerNames().stream().noneMatch(pastName -> pastName.getName().equals(name)))
        {
            PastPlayerNames pastName = new PastPlayerNames(name, player);
            player.getPlayerNames().add(pastName);
        }
    }

    private String getReadableDateInUTC(Battle battle) {
        return Instant.ofEpochSecond(battle.getBattleAt())
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm 'UTC'"));
    }

    private String getPlayerName(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1Name() : battle.getPlayer2Name();
    }


    private String getPlayerCharacter(Battle battle, int playerNumber) {
        return playerNumber == 1 ? String.valueOf(battle.getPlayer1CharacterID()) : String.valueOf(battle.getPlayer2CharacterID());
    }


    private String getPlayerUserId(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1UserID() : battle.getPlayer2UserID();
    }

    private String getPlayerPolarisId(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1PolarisID() : battle.getPlayer2PolarisID();
    }

    private long getPlayerTekkenPower(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1TekkenPower() : battle.getPlayer2TekkenPower();
    }

    private int getPlayerDanRank(Battle battle, int playerNumber) {
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
