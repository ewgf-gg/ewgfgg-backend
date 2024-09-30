package org.tekkenstats.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.OptimisticLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.tekkenstats.Battle;
import org.tekkenstats.CharacterStats;
import org.tekkenstats.PastPlayerNames;
import org.tekkenstats.Player;
import org.tekkenstats.configuration.RabbitMQConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tekkenstats.repositories.BattleRepository;
import org.tekkenstats.repositories.PastPlayerNamesRepository;
import org.tekkenstats.repositories.PlayerRepository;


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
    private BattleRepository battleRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private PastPlayerNamesRepository pastPlayerNamesRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME, concurrency = "4")
    @Transactional
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

    public void processBattlesAsync(List<Battle> battles)
    {
        // Extract battle IDs and player IDs
        Set<String> battleIDs = new HashSet<>();
        Set<String> playerIDs = new HashSet<>();
        extractBattleAndPlayerIDs(battles, battleIDs, playerIDs);

        // Fetch existing battles and players
        Map<String, Battle> mapOfExistingBattles = fetchExistingBattles(battleIDs);
        Map<String, Player> mapOfExistingPlayers = fetchExistingPlayers(playerIDs);

        Set<Player> updatedPlayers = new HashSet<>();
        Set<Battle> battleSet = new HashSet<>();

        // Instantiate objects and update relevant information
        processBattlesAndPlayers(battles, mapOfExistingBattles, mapOfExistingPlayers, updatedPlayers,battleSet);

        // Execute battle bulk operations
        executeBattleBatchWrite(battleSet);

        // Execute player bulk operations
        executePlayerBulkOperations(mapOfExistingPlayers, updatedPlayers);
    }

    private void extractBattleAndPlayerIDs(List<Battle> battles, Set<String> battleIDs, Set<String> playerIDs)
    {
        for (Battle battle : battles) {
            battleIDs.add(battle.getBattleId());
            playerIDs.add(battle.getPlayer1UserID());
            playerIDs.add(battle.getPlayer2UserID());
        }
    }

    private Map<String, Battle> fetchExistingBattles(Set<String> battleIDs)
    {
        long startTime = System.currentTimeMillis();

        // If battleIDs is empty, we can skip the query
        if (battleIDs.isEmpty()) {
            logger.warn("No battle IDs provided. Skipping database fetch.");
            return Collections.emptyMap();
        }

        // Fetch existing battles from PostgreSQL using JPA or native queries
        List<Battle> existingBattles = battleRepository.findAllByBattleIdIn(battleIDs);

        long endTime = System.currentTimeMillis();
        logger.info("Retrieved existing battles from database: {} ms", (endTime - startTime));

        // Collect the results into a Map for easy lookup
        return existingBattles.stream()
                .collect(Collectors.toMap(Battle::getBattleId, battle -> battle));
    }


    private Map<String, Player> fetchExistingPlayers(Set<String> playerIDs) {

        long startTime = System.currentTimeMillis();

        // If battleIDs is empty, we can skip the query
        if (playerIDs.isEmpty()) {
            logger.warn("No player IDs provided. Skipping database fetch.");
            return Collections.emptyMap();
        }

        // Fetch existing battles from PostgreSQL using JPA or native queries
        List<Player> existingBattles = playerRepository.findAllByPlayerIdIn(playerIDs);

        long endTime = System.currentTimeMillis();
        logger.info("Retrieved existing players from database: {} ms", (endTime - startTime));

        // Collect the results into a Map for easy lookup
        return existingBattles.stream()
                .collect(Collectors.toMap(Player::getPlayerId, player -> player));
    }

    private void processBattlesAndPlayers(
            List<Battle> battles,
            Map<String, Battle> existingBattlesMap,
            Map<String, Player> existingPlayersMap,
            Set<Player> updatedPlayers,
            Set<Battle> battleSet)
    {

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
                battleSet.add(battle);
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

    private void executeBattleBatchWrite(Set<Battle> battleSet)
    {
        if (battleSet == null || battleSet.isEmpty()) {
            logger.warn("No battles to insert or update.");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();

            // Save all battles in one go (JPA will handle batching if configured)
            battleRepository.saveAll(battleSet);

            long endTime = System.currentTimeMillis();
            logger.info("Battle Insertion: {} ms, Inserted/Updated: {}", (endTime - startTime), battleSet.size());

        } catch (Exception e) {
            logger.error("BATTLE INSERTION FAILED: ", e);
        }
    }


    public void executePlayerBulkOperations(
            Map<String, Player> existingPlayersMap,
            Set<Player> updatedPlayersSet )

    {

        if (updatedPlayersSet.isEmpty()) {
            logger.warn("Updated Player Set is empty! (Battle batch already existed in database)");
            return;
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;
        int retryCount = 0;
        final int maxRetries = 3;

        // Bulk processing with retry logic
        while (!success && retryCount < maxRetries)
        {
            try {
                // Bulk save players
                // Bulk save new player names
                Set<PastPlayerNames> newPlayerNames = updatedPlayersSet.stream()
                        .flatMap(player -> player.getPlayerNames().stream())
                        .collect(Collectors.toSet());

                playerRepository.saveAll(updatedPlayersSet);

                success = true;
            } catch (OptimisticLockException e) {
                retryCount++;
                logger.warn("Optimistic lock exception: Retrying bulk operation (Attempt {}/{})", retryCount, maxRetries);

                if (retryCount >= maxRetries) {
                    logger.error("Max retry limit reached. Aborting bulk operation.");
                    throw e;
                }

                // Optionally refresh the entities from the database
                updatedPlayersSet = refreshPlayersFromDatabase(updatedPlayersSet);
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("Player Bulk Upsert: {} ms, Processed Players: {}",
                (endTime - startTime), updatedPlayersSet.size());
    }

    private Set<Player> refreshPlayersFromDatabase(Set<Player> updatedPlayersSet)
    {
        Set<Player> refreshedPlayers = new HashSet<>();
        for (Player player : updatedPlayersSet)
        {
            Player refreshedPlayer = playerRepository.findById(player.getPlayerId()).orElseThrow();
            refreshedPlayers.add(refreshedPlayer);
        }
        return refreshedPlayers;
    }




    private Player getOrCreatePlayer(Map<String, Player> playerMap, Battle battle, int playerNumber) {
        String userId = playerNumber == 1 ? battle.getPlayer1UserID() : battle.getPlayer2UserID();
        Player player = playerMap.get(userId);

        if (player != null)
        {
            return updateExistingPlayer(player, battle, playerNumber);
        }
        else
        {
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
            stats.setRating(calculatePlayerRating(battle, playerNumber));
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
        characterStats.setRating(calculatePlayerRating(battle, playerNumber));
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
