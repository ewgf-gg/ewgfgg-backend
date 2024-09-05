package org.tekkenstats.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tekkenstats.Battle;
import org.tekkenstats.Player;
import org.tekkenstats.interfaces.BattleRepository;
import org.tekkenstats.interfaces.PlayerRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReplayService {

    private static final Logger logger = LogManager.getLogger(ReplayService.class);

    @Autowired
    private BattleRepository battleRepository;
    @Autowired
    private PlayerRepository playerRepository;

    @Transactional
    public void processBattles(List<Battle> battles)
    {
        Set<String> battleIDs = new HashSet<>();
        Set<String> playerIDs = new HashSet<>();

        long startTime = System.currentTimeMillis();

        for (Battle battle : battles) {
            battleIDs.add(battle.getBattleId());
            playerIDs.add(battle.getPlayer1UserID());
            playerIDs.add(battle.getPlayer2UserID());
        }
        long endTime = System.currentTimeMillis();


        logger.error("Fetched info from JSON: {} ms", (endTime - startTime));
        // Bulk read battles and players
        startTime = System.currentTimeMillis();

        List<Battle> existingBattles = battleRepository.findAllById(battleIDs);
        List<Player> existingPlayers = playerRepository.findAllById(playerIDs);
        endTime = System.currentTimeMillis();

        logger.error("Retrieved info from database: {} ms", (endTime -startTime));
        // Create maps for quick lookups
        startTime = System.currentTimeMillis();

        Map<String, Battle> existingBattleMap = existingBattles.stream()
                .collect(Collectors.toMap(Battle::getBattleId, battle -> battle));
        Map<String, Player> playerMap = existingPlayers.stream()
                .collect(Collectors.toMap(Player::getUserId, player -> player));
        endTime = System.currentTimeMillis();


        logger.error("Mapped data to objects: {} ms", (endTime-startTime));
        List<Battle> newBattles = new ArrayList<>();
        Set<Player> updatedPlayers = new HashSet<>();

        startTime = System.currentTimeMillis();

        for (Battle battle : battles) {
            if (!existingBattleMap.containsKey(battle.getBattleId())) {
                battle.setDate(getReadableDateInUTC(battle));
                Player player1 = getOrCreatePlayer(playerMap, battle, 1);
                Player player2 = getOrCreatePlayer(playerMap, battle, 2);

                boolean isNewBattleForPlayer1 = isNewBattle(battle, player1);
                boolean isNewBattleForPlayer2 = isNewBattle(battle, player2);

                updatePlayerWithBattle(player1, battle, isNewBattleForPlayer1, 1);
                updatePlayerWithBattle(player2, battle, isNewBattleForPlayer2, 2);

                updatedPlayers.add(player1);
                updatedPlayers.add(player2);
                newBattles.add(battle);
            } else {
                logger.info("battleId: {} already exists in databse, skipping write", battle.getBattleId());
            }
        }
        endTime = System.currentTimeMillis();


        logger.error("Updated player and battle information: {} ms", (endTime-startTime));


        if (!newBattles.isEmpty()) {
            startTime = System.currentTimeMillis();
            battleRepository.saveAll(newBattles);
            endTime = System.currentTimeMillis();
            logger.error("Battle Insertion: {} ms", (endTime-startTime));

            startTime = System.currentTimeMillis();
            playerRepository.saveAll(updatedPlayers);
            endTime = System.currentTimeMillis();

            logger.error("Player Insertion: {} ms", (endTime-startTime));
        }



    }


    private boolean isNewBattle(Battle battle, Player player)
    {
        List<Battle> last10Battles = player.getLast10Battles();
        if (last10Battles.isEmpty())
        {
            return true;  // If no battles, consider it new
        }
        Battle newestBattle = last10Battles.get(0);
        return battle.getBattleAt() > newestBattle.getBattleAt();
    }

    private Player getOrCreatePlayer(Map<String, Player> playerMap, Battle battle, int playerNumber) {
        String userId = playerNumber == 1 ? battle.getPlayer1UserID() : battle.getPlayer2UserID();
        logger.info("Attempting to retrieve player{} info...", playerNumber);

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
        logger.info("Player information found in Database! Updating...");
        addPlayerNameIfNew(player, getPlayerName(battle, playerNumber));
        return player;
    }

    private Player createNewPlayer(Battle battle, int playerNumber)
    {
        logger.info("Player information not found. Creating new Player object");
        Player player = new Player();
        player.setUserId(getPlayerUserId(battle, playerNumber));
        player.setLosses(0);
        player.setWins(0);
        player.setPlayerNames(new ArrayList<>());
        player.setLast10Battles(new ArrayList<>());
        updatePlayerDetails(player, battle, playerNumber);
        return player;
    }

    private void updatePlayerWithBattle(Player player, Battle battle, boolean isNewBattle, int playerNumber)
    {
        updateLast10Battles(player, battle);
        updateWinsAndLosses(player, battle.getWinner(), playerNumber);
        updateWinRate(player);

        if (isNewBattle)
        {
            updatePlayerDetails(player, battle, playerNumber);
        }

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

    private void updateLast10Battles(Player player, Battle battle)
    {
        List<Battle> last10Battles = player.getLast10Battles();
        if (battle.getDate() == null || battle.getDate().isEmpty())
        {
            battle.setDate(getReadableDateInUTC(battle));
        }
        // Add the new battle
        last10Battles.add(battle);

        // Sort the list in descending order
        last10Battles.sort((b1, b2) -> Long.compare(b2.getBattleAt(), b1.getBattleAt()));

        // Keep only the first 10 elements
        if (last10Battles.size() > 10) {
            last10Battles.remove(last10Battles.size()-1);
        }
    }

    private String getReadableDateInUTC(Battle battle)
    {
        return Instant.ofEpochSecond(battle.getBattleAt())
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm 'UTC'"));
    }

    private void updateWinsAndLosses(Player player, int winner, int playerNumber)
    {
        if (winner == playerNumber)
        {
            player.setWins(player.getWins() + 1);
        }
        else
        {
            player.setLosses(player.getLosses()+1);
        }
    }

    private void updateWinRate(Player player)
    {
        float winRate = (player.getWins() + player.getLosses() > 0) ? (player.getWins() / (float) (player.getWins() + player.getLosses()) * 100) : 0;
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