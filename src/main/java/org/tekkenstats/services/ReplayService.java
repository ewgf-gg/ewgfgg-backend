package org.tekkenstats.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.bulk.BulkWriteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.tekkenstats.Battle;
import org.tekkenstats.Player;
import org.tekkenstats.configuration.RabbitMQConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReplayService {

    private static final Logger logger = LogManager.getLogger(ReplayService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MongoTemplate mongoTemplate;


    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME, concurrency = "5")
    @Retryable(
            value = { Exception.class }, // Retry for any Exception
            maxAttempts = 5, // Retry up to 5 times
            backoff = @Backoff(delay = 1000, multiplier = 2)) // Initial delay of 2s, increasing with each retry
    public void receiveMessage(String message, @Header("unixTimestamp") String dateAndTime) throws JsonProcessingException
    {
        String threadName = Thread.currentThread().getName();
        logger.info("Thread: {}, Received Battle Data from RabbitMQ, Timestamped: {}", threadName, dateAndTime);

        long startTime = System.currentTimeMillis();

        List<Battle> battles = objectMapper.readValue(message, new TypeReference<List<Battle>>() {
        });
        processBattlesAsync(battles);

        long endTime = System.currentTimeMillis();

        logger.info("Thread: {}, Total Operation Time: {} ms", threadName, endTime - startTime);

    }


    @Async("taskExecutor")
    public void processBattlesAsync(List<Battle> battles) {
        // Extract battle IDs and player IDs
        Set<String> battleIDs = new HashSet<>();
        Set<String> playerIDs = new HashSet<>();
        extractBattleAndPlayerIDs(battles, battleIDs, playerIDs);

        // Fetch existing battles and players
        Map<String, Battle> mapOfExistingBattles = fetchExistingBattles(battleIDs);
        Map<String, Player> mapOfExistingPlayers = fetchExistingPlayers(playerIDs);

        Set<Player> updatedPlayers = new HashSet<>();

        // Initialize bulk operations
        BulkOperations battleBulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Battle.class);
        BulkOperations playerBulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Player.class);

        // Process battles
        processBattlesAndPlayers(battles, mapOfExistingBattles, mapOfExistingPlayers, updatedPlayers, battleBulkOps);

        // Execute battle bulk operations
        executeBattleBulkOperations(battleBulkOps);

        // Execute player bulk operations
        executePlayerBulkOperations(mapOfExistingPlayers, updatedPlayers, playerBulkOps);
    }

    private void extractBattleAndPlayerIDs(List<Battle> battles, Set<String> battleIDs, Set<String> playerIDs) {
        for (Battle battle : battles) {
            battleIDs.add(battle.getBattleId());
            playerIDs.add(battle.getPlayer1UserID());
            playerIDs.add(battle.getPlayer2UserID());
        }
    }

    private Map<String, Battle> fetchExistingBattles(Set<String> battleIDs) {
        long startTime = System.currentTimeMillis();

        Query battleQuery = new Query(Criteria.where("battleId").in(battleIDs));
        List<Battle> existingBattles = mongoTemplate.find(battleQuery, Battle.class);

        long endTime = System.currentTimeMillis();
        logger.info("Retrieved existing battles from database: {} ms", (endTime - startTime));

        return existingBattles.stream()
                .collect(Collectors.toMap(Battle::getBattleId, battle -> battle));
    }

    private Map<String, Player> fetchExistingPlayers(Set<String> playerIDs) {
        long startTime = System.currentTimeMillis();

        Query playerQuery = new Query(Criteria.where("userId").in(playerIDs));
        List<Player> existingPlayers = mongoTemplate.find(playerQuery, Player.class);

        long endTime = System.currentTimeMillis();
        logger.info("Retrieved existing players from database: {} ms", (endTime - startTime));

        return existingPlayers.stream()
                .collect(Collectors.toMap(Player::getUserId, player -> player));
    }

    private void processBattlesAndPlayers(
            List<Battle> battles,
            Map<String, Battle> existingBattlesMap,
            Map<String, Player> existingPlayersMap,
            Set<Player> updatedPlayers,
            BulkOperations battleBulkOps) {

        long startTime = System.currentTimeMillis();
        int duplicateBattles = 0;

        for (Battle battle : battles) {
            if (!existingBattlesMap.containsKey(battle.getBattleId())) {
                battle.setDate(getReadableDateInUTC(battle));

                Player player1 = getOrCreatePlayer(existingPlayersMap, battle, 1);
                Player player2 = getOrCreatePlayer(existingPlayersMap, battle, 2);

                updatePlayerWithBattle(player1, battle, 1);
                updatePlayerWithBattle(player2, battle, 2);

                updatedPlayers.add(player1);
                updatedPlayers.add(player2);

                // Queue insert operation for battle
                battleBulkOps.insert(battle);
            } else {
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

    private void executeBattleBulkOperations(BulkOperations battleBulkOps) {
        try {
            long startTime = System.currentTimeMillis();
            BulkWriteResult battleResult = battleBulkOps.execute();
            long endTime = System.currentTimeMillis();

            logger.info("Battle Insertion: {} ms, Inserted: {}",
                    (endTime - startTime), battleResult.getInsertedCount());
        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("state should be: writes is not an empty list")) {
                logger.warn("BATTLE INSERTION FAILED, BATTLES ALREADY EXIST IN DATABASE");
            }
        }

    }

    private void executePlayerBulkOperations(
            Map<String, Player> existingPlayersMap,
            Set<Player> updatedPlayersSet,
            BulkOperations playerBulkOps) {
        if (updatedPlayersSet.isEmpty()) {
            logger.warn("Updated Player Set is empty! (Battle batch already existed in database)");
            return;
        }

        long startTime = System.currentTimeMillis();

        for (Player updatedPlayer : updatedPlayersSet) {
            String userId = updatedPlayer.getUserId();
            Player existingPlayer = existingPlayersMap.get(userId);
            Query query = new Query(Criteria.where("userId").is(userId));
            Update update = new Update();

            // Ensure playerNames is initialized
            if (updatedPlayer.getPlayerNames() == null) {
                updatedPlayer.setPlayerNames(new ArrayList<>());
            }

            // Handle character stats
            Map<String, Player.CharacterStats> updatedCharacterStats = updatedPlayer.getCharacterStats();

            if (updatedCharacterStats != null) {
                for (Map.Entry<String, Player.CharacterStats> entry : updatedCharacterStats.entrySet()) {
                    String characterName = entry.getKey();
                    Player.CharacterStats updatedStats = entry.getValue();

                    // Build update operations for character stats
                    String characterStatsPath = "characterStats." + characterName;

                    // Only update if it's the latest battle
                    boolean isLatestBattle = true;
                    if (existingPlayer != null) {
                        Player.CharacterStats existingStats = existingPlayer.getCharacterStats().get(characterName);
                        if (existingStats != null) {
                            isLatestBattle = existingStats.getLatestBattle() <= updatedStats.getLatestBattle();
                        }
                    }

                    if (isLatestBattle) {
                        update.set(characterStatsPath + ".danRank", updatedStats.getDanRank())
                                .set(characterStatsPath + ".latestBattle", updatedStats.getLatestBattle());
                               // .inc(characterStatsPath + ".rating", updatedStats.getRating());
                    }

                    // Increment wins and losses
                    update.inc(characterStatsPath + ".wins", updatedStats.getWinsIncrement())
                            .inc(characterStatsPath + ".losses", updatedStats.getLossIncrement());

                    // Reset increments after processing
                    updatedStats.setLossIncrement(0);
                    updatedStats.setWinsIncrement(0);
                    updatedStats.setRatingChange(0);
                }
            }

            // Update player-level fields
            update.addToSet("playerNames", updatedPlayer.getName());

            // Set on insert fields
            update.setOnInsert("userId", userId)
                    .setOnInsert("name", updatedPlayer.getName())
                    .setOnInsert("polarisId", updatedPlayer.getPolarisId())
                    .setOnInsert("tekkenPower", updatedPlayer.getTekkenPower());

            playerBulkOps.upsert(query, update);
        }

        BulkWriteResult playerResult = playerBulkOps.execute();
        long endTime = System.currentTimeMillis();
        logger.info("Player Upsert: {} ms, Upserted: {}, Modified: {}",
                (endTime - startTime), playerResult.getUpserts().size(), playerResult.getModifiedCount());
    }


    private Player getOrCreatePlayer(Map<String, Player> playerMap, Battle battle, int playerNumber) {
        String userId = playerNumber == 1 ? battle.getPlayer1UserID() : battle.getPlayer2UserID();
        Player player = playerMap.get(userId);
        if (player != null) {
            return updateExistingPlayer(player, battle, playerNumber);
        } else {
            Player newPlayer = createNewPlayer(battle, playerNumber);
            playerMap.put(userId, newPlayer);
            return newPlayer;
        }
    }

    private Player updateExistingPlayer(Player player, Battle battle, int playerNumber) {
        addPlayerNameIfNew(player, getPlayerName(battle, playerNumber));
        return player;
    }

    private Player createNewPlayer(Battle battle, int playerNumber)
    {
        Player newPlayer = new Player();
        updateNewPlayerDetails(newPlayer, battle, playerNumber);
        return newPlayer;
    }

    private void updatePlayerWithBattle(Player player, Battle battle, int playerNumber)
    {
        Player.CharacterStats stats = player.getCharacterStats().get(getPlayerCharacter(battle, playerNumber));
        // Initialize stats if null
        if (stats == null) {
            stats = new Player.CharacterStats();
            stats.setDanRank(getPlayerDanRank(battle, playerNumber));
            stats.setRating(calculatePlayerRating(battle, playerNumber));
            player.getCharacterStats().put(getPlayerCharacter(battle, playerNumber), stats);
        }

        if (battle.getWinner() == playerNumber) {
            stats.setWinsIncrement(stats.getWinsIncrement() + 1);
            stats.setWins(stats.getWins() + 1);
        } else {
            stats.setLossIncrement(stats.getLossIncrement() + 1);
            stats.setLosses(stats.getLosses() + 1);
        }

        if (battle.getBattleAt() > stats.getLatestBattle()) {
            stats.setLatestBattle(battle.getBattleAt());
        }

        stats.setRatingChange(playerNumber == 1 ? battle.getPlayer1RatingChange() : battle.getPlayer2RatingChange());
        //updateWinRate(player);
    }

    private void updateNewPlayerDetails(Player player, Battle battle, int playerNumber) {
        player.setUserId(getPlayerUserId(battle, playerNumber));
        player.setName(getPlayerName(battle, playerNumber));
        player.setPolarisId(getPlayerPolarisId(battle, playerNumber));
        player.setTekkenPower(getPlayerTekkenPower(battle, playerNumber));

        Player.CharacterStats characterStats = new Player.CharacterStats();
        characterStats.setDanRank(getPlayerDanRank(battle, playerNumber));
        characterStats.setRating(calculatePlayerRating(battle, playerNumber));
        player.getCharacterStats().put(getPlayerCharacter(battle, playerNumber), characterStats);

        addPlayerNameIfNew(player, player.getName());
    }

    private void addPlayerNameIfNew(Player player, String name) {
        if (player.getPlayerNames() == null) {
            player.setPlayerNames(new ArrayList<>());
        }
        if (!player.getPlayerNames().contains(name)) {
            player.getPlayerNames().add(name);
        }
    }


    private String getReadableDateInUTC(Battle battle) {
        return Instant.ofEpochSecond(battle.getBattleAt())
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm 'UTC'"));
    }

//    private void updateWinRate(Player player) {
//        double winRate = (player.getWins() + player.getLosses() > 0) ? (player.getWins() / (float) (player.getWins() + player.getLosses()) * 100) : 0;
//        player.setWinRate(winRate);
//    }

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
