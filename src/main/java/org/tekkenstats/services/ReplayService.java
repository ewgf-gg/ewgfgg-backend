package org.tekkenstats.services;

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
    public void receiveMessage(String message, @Header("unixTimestamp") String dateAndTime) throws Exception
    {
        logger.info("Received Battle Data from RabbitMQ, Timestamped: {}", dateAndTime );
            long startTime = System.currentTimeMillis();

            List<Battle> battles = objectMapper.readValue(message, new TypeReference<List<Battle>>() {});
            processBattlesAsync(battles);
            long endTime = System.currentTimeMillis();

            logger.info("Total Operation Time: {} ms", endTime - startTime);

        }


    @Async("taskExecutor")
    public void processBattlesAsync(List<Battle> battles)
    {
        // Extract battle IDs and player IDs
        Set<String> battleIDs = new HashSet<>();
        Set<String> playerIDs = new HashSet<>();
        extractBattleAndPlayerIDs(battles, battleIDs, playerIDs);

        // Fetch existing battles and players
        Map<String, Battle> existingBattleMap = fetchExistingBattles(battleIDs);
        Map<String, Player> playerMap = fetchExistingPlayers(playerIDs);

        Set<Player> updatedPlayers = new HashSet<>();

        // Initialize bulk operations
        BulkOperations battleBulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Battle.class);
        BulkOperations playerBulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Player.class);

        // Process battles
        processBattlesAndPlayers(battles, existingBattleMap, playerMap, updatedPlayers, battleBulkOps);

        // Execute battle bulk operations
        executeBattleBulkOperations(battleBulkOps);

        // Execute player bulk operations
        executePlayerBulkOperations(playerMap, updatedPlayers, playerBulkOps);
    }

    private void extractBattleAndPlayerIDs(List<Battle> battles, Set<String> battleIDs, Set<String> playerIDs)
    {
        for (Battle battle : battles)
        {
            battleIDs.add(battle.getBattleId());
            playerIDs.add(battle.getPlayer1UserID());
            playerIDs.add(battle.getPlayer2UserID());
        }
    }

    private Map<String, Battle> fetchExistingBattles(Set<String> battleIDs)
    {
        long startTime = System.currentTimeMillis();

        Query battleQuery = new Query(Criteria.where("battleId").in(battleIDs));
        List<Battle> existingBattles = mongoTemplate.find(battleQuery, Battle.class);

        long endTime = System.currentTimeMillis();
        logger.info("Retrieved existing battles from database: {} ms", (endTime - startTime));

        return existingBattles.stream()
                .collect(Collectors.toMap(Battle::getBattleId, battle -> battle));
    }

    private Map<String, Player> fetchExistingPlayers(Set<String> playerIDs)
    {
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
            Map<String, Battle> existingBattleMap,
            Map<String, Player> playerMap,
            Set<Player> updatedPlayers,
            BulkOperations battleBulkOps)
    {

        long startTime = System.currentTimeMillis();
        int duplicateBattles = 0;

        for (Battle battle : battles) {
            if (!existingBattleMap.containsKey(battle.getBattleId())) {
                battle.setDate(getReadableDateInUTC(battle));

                Player player1 = getOrCreatePlayer(playerMap, battle, 1);
                Player player2 = getOrCreatePlayer(playerMap, battle, 2);

                updatePlayerWithBattle(player1, battle, 1);
                updatePlayerWithBattle(player2, battle, 2);

                updatedPlayers.add(player1);
                updatedPlayers.add(player2);

                // Queue insert operation for battle
                battleBulkOps.insert(battle);
            } else
            {
                duplicateBattles++;
            }
        }
        long endTime = System.currentTimeMillis();
        if (duplicateBattles == battles.size())
        {
            logger.warn("Entire batch already exists in database!");
            return;
        }
        logger.info("Updated player and battle information: {} ms", (endTime - startTime));
    }

    private void executeBattleBulkOperations(BulkOperations battleBulkOps)
    {
        try
        {
            long startTime = System.currentTimeMillis();
            BulkWriteResult battleResult = battleBulkOps.execute();
            long endTime = System.currentTimeMillis();

            logger.info("Battle Insertion: {} ms, Inserted: {}",
                    (endTime - startTime), battleResult.getInsertedCount());
        }
        catch(IllegalArgumentException e)
        {
            if (e.getMessage().equals("state should be: writes is not an empty list"))
            {
                logger.warn("BATTLE INSERTION FAILED, BATTLES ALREADY EXIST IN DATABASE");
            }
        }

    }
    private void executePlayerBulkOperations(
            Map<String, Player> playerMap,
            Set<Player> updatedPlayers,
            BulkOperations playerBulkOps)
    {

        if (!updatedPlayers.isEmpty()) {
            long startTime = System.currentTimeMillis();

            for (Player player : updatedPlayers) {
                Player existingPlayer = playerMap.get(player.getUserId());
                Query query = new Query(Criteria.where("userId").is(player.getUserId()));
                Update update = new Update();

                // Update only if this is the latest battle
                if (existingPlayer == null || existingPlayer.getLatestBattle() < player.getLatestBattle()) {
                    update.set("danRank", player.getDanRank())
                            .set("tekkenPower", player.getTekkenPower())
                            .set("latestBattle", player.getLatestBattle())
                            .inc("rating", player.getRatingChange());
                }
                //  setOnInsert for new documents
                update.setOnInsert("userId", player.getUserId())
                        .setOnInsert("name", player.getName())
                        .setOnInsert("danRank", player.getDanRank())
                        .setOnInsert("tekkenPower", player.getTekkenPower())
                        .setOnInsert("polarisId", player.getPolarisId())
                        .setOnInsert("rating", player.getRating());

                //increment if player already exists
                update.inc("wins", player.getWinsIncrement())
                        .inc("losses", player.getLossIncrement())
                        .addToSet("playerNames", player.getName());

                playerBulkOps.upsert(query, update);

                // Reset increments after processing
                player.setLossIncrement(0);
                player.setWinsIncrement(0);
                player.setRatingChange(0);
            }

            BulkWriteResult playerResult = playerBulkOps.execute();
            long endTime = System.currentTimeMillis();
            logger.info("Player Upsert: {} ms, Upserted: {}, Modified: {}",
                    (endTime - startTime), playerResult.getUpserts().size(), playerResult.getModifiedCount());
        }
    }

    private Player getOrCreatePlayer(Map<String, Player> playerMap, Battle battle, int playerNumber)
    {
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

    private Player updateExistingPlayer(Player player, Battle battle, int playerNumber)
    {
        addPlayerNameIfNew(player, getPlayerName(battle, playerNumber));
        return player;
    }

    private Player createNewPlayer(Battle battle, int playerNumber)
    {
        Player player = new Player();
        player.setUserId(getPlayerUserId(battle, playerNumber));
        player.setLosses(0);
        player.setWins(0);
        player.setPlayerNames(new ArrayList<>());
        player.setLatestBattle(0);
        updatePlayerDetails(player, battle, playerNumber);
        return player;
    }

    private void updatePlayerWithBattle(Player player, Battle battle, int playerNumber) {

        if (battle.getWinner() == playerNumber)
        {
            player.setWinsIncrement(player.getWinsIncrement() + 1);
            player.setWins(player.getWins() + 1);
        } else
        {
            player.setLossIncrement(player.getLossIncrement() + 1);
            player.setLosses(player.getLosses() + 1);
        }

        if(battle.getBattleAt() > player.getLatestBattle())
        {
            player.setLatestBattle(battle.getBattleAt());
        }

        player.setRatingChange(playerNumber == 1 ? battle.getPlayer1RatingChange() : battle.getPlayer2RatingChange());
        updateWinRate(player);
    }

    private void updatePlayerDetails(Player player, Battle battle, int playerNumber)
    {
        player.setName(getPlayerName(battle, playerNumber));
        player.setPolarisId(getPlayerPolarisId(battle, playerNumber));
        player.setTekkenPower(getPlayerTekkenPower(battle, playerNumber));
        player.setDanRank(getPlayerDanRank(battle, playerNumber));
        player.setRating(calculatePlayerRating(battle, playerNumber));
        addPlayerNameIfNew(player, player.getName());
    }

    private void addPlayerNameIfNew(Player player, String name)
    {
        if (player.getPlayerNames() == null) {
            player.setPlayerNames(new ArrayList<>());
        }
        if (!player.getPlayerNames().contains(name)) {
            player.getPlayerNames().add(name);
        }
    }


    private String getReadableDateInUTC(Battle battle)
    {
        return Instant.ofEpochSecond(battle.getBattleAt())
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm 'UTC'"));
    }

    private void updateWinRate(Player player)
    {
        double winRate = (player.getWins() + player.getLosses() > 0) ? (player.getWins() / (float) (player.getWins() + player.getLosses()) * 100) : 0;
        player.setWinRate(winRate);
    }

    private String getPlayerName(Battle battle, int playerNumber) {
        return playerNumber == 1 ? battle.getPlayer1Name() : battle.getPlayer2Name();
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