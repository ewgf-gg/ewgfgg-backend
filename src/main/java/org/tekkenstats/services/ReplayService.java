package org.tekkenstats.services;

import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.tekkenstats.Battle;
import org.tekkenstats.Player;
import org.tekkenstats.interfaces.BattleRepository;
import org.tekkenstats.interfaces.PlayerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReplayService {

    private static final Logger logger = LogManager.getLogger(ReplayService.class);

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private PlayerRepository playerRepository;

    public void saveBattleData(Battle battle) {


        // API can return null for these fields, these are safeguards
        Integer player1RatingBefore = battle.getPlayer1RatingBefore() != null ? battle.getPlayer1RatingBefore() : 0;
        Integer player1RatingChange = battle.getPlayer1RatingChange() != null ? battle.getPlayer1RatingChange() : 0;

        logger.info("Attempting to retrieve player1 info...");
        Player player1 = getOrCreatePlayer(
                battle.getPlayer1UserID(),
                battle.getPlayer1Name(),
                battle.getPlayer1PolarisID(),
                battle.getPlayer1TekkenPower(),
                battle.getPlayer1DanRank(),
                player1RatingBefore + player1RatingChange);

        // API can return null for these fields, these are safeguards
        Integer player2RatingBefore = battle.getPlayer2RatingBefore() != null ? battle.getPlayer2RatingBefore() : 0;
        Integer player2RatingChange = battle.getPlayer2RatingChange() != null ? battle.getPlayer2RatingChange() : 0;

        // Save or update player2
        Player player2 = getOrCreatePlayer(
                battle.getPlayer2UserID(),
                battle.getPlayer2Name(),
                battle.getPlayer2PolarisID(),
                battle.getPlayer2TekkenPower(),
                battle.getPlayer2DanRank(),
                player2RatingBefore + player2RatingChange);

        updatePlayerWithBattle(player1, player2, battle);


        // Save the battle
        battleRepository.save(battle);
    }

    private Player getOrCreatePlayer(String userId, String name, String polarisId, long tekkenPower, int danRank, int rating) {
        Optional<Player> playerOptional = playerRepository.findById(userId);
        Player player;


        if (playerOptional.isPresent()) {
            logger.info("Player information found in Database! Retrieving...");
            player = playerOptional.get();

            if (player.getPlayerNames() == null)
            {
                player.setPlayerNames(new ArrayList<>());
            }

            if (!player.getPlayerNames().contains(name))
            {
                player.getPlayerNames().add(name);
            }
        } else {
            logger.info("Player information not found. Creating new Player object");
            player = new Player();
            player.setUserId(userId);
            player.setRating(rating);
            player.setLosses(0);
            player.setWins(0);
            player.setPlayerNames(new ArrayList<>());
            player.getPlayerNames().add(name);
            player.setLast10Battles(new ArrayList<>());

        }

        // Update player's details with the latest information
        player.setName(name);
        player.setPolarisId(polarisId);
        player.setTekkenPower(tekkenPower);
        player.setDanRank(danRank);
        player.setRating(rating);


        return playerRepository.save(player);
    }

    private void updatePlayerWithBattle(Player player1, Player player2, Battle battle) {
        // Update player's last 10 battles list
        List<Battle> last10BattlesPlayer1 = player1.getLast10Battles();
        if (last10BattlesPlayer1.size() >= 10) {
            last10BattlesPlayer1.remove(0); // Remove the oldest battle if we already have 10
        }
        last10BattlesPlayer1.add(battle);
        player1.setLast10Battles(last10BattlesPlayer1);

        List<Battle> last10BattlesPlayer2 = player2.getLast10Battles();
        if (last10BattlesPlayer2.size() >= 10) {
            last10BattlesPlayer2.remove(0); // Remove the oldest battle if we already have 10
        }
        last10BattlesPlayer2.add(battle);
        player2.setLast10Battles(last10BattlesPlayer2);

        if(battle.getWinner() == 1)
        {
            player1.setWins(player1.getWins()+1);
            player2.setLosses(player2.getLosses()+1);
        }
        else if(battle.getWinner() == 2) //have to specify in the case of a draw
        {
            player2.setWins(player2.getWins()+1);
            player1.setLosses(player1.getLosses()+1);
        }

        if (player1.getWins() + player1.getLosses() > 0)
        {
            player1.setWinRate(player1.getWins() / (float) (player1.getWins() + player1.getLosses()) * 100);
        } else
        {
            player1.setWinRate(0); // Safeguard for division by zero
        }

        if (player2.getWins() + player2.getLosses() > 0)
        {
            player2.setWinRate(player2.getWins() / (float) (player2.getWins() + player2.getLosses()) * 100);
        } else
        {
            player2.setWinRate(0); // Safeguard for division by zero
        }



        logger.info("Saving Player 1 Information into Database: {}", player1.getName());
        // Save the updated player information
        playerRepository.save(player1);
        logger.info("Saving Player 2 Information into Database: {}", player2.getName());
        playerRepository.save(player2);
    }


}
