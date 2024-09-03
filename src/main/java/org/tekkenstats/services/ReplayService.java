package org.tekkenstats.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tekkenstats.Battle;
import org.tekkenstats.Player;
import org.tekkenstats.interfaces.BattleRepository;
import org.tekkenstats.interfaces.PlayerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ReplayService {

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private PlayerRepository playerRepository;

    public void saveBattleData(Battle battle) {
        // Save or update player1

        // API can return null for these fields, these are safeguards
        Integer player1RatingBefore = battle.getPlayer1RatingBefore() != null ? battle.getPlayer1RatingBefore() : 0;
        Integer player1RatingChange = battle.getPlayer1RatingChange() != null ? battle.getPlayer1RatingChange() : 0;

        Player player1 = getOrCreatePlayer(
                battle.getPlayer1UserID(),
                battle.getPlayer1Name(),
                battle.getPlayer1PolarisID(),
                battle.getPlayer1TekkenPower(),
                battle.getPlayer1DanRank(),
                player1RatingBefore + player1RatingChange);


        updatePlayerWithBattle(player1, battle);

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

        updatePlayerWithBattle(player2, battle);

        // Save the battle
        battleRepository.save(battle);
    }

    private Player getOrCreatePlayer(String userId, String name, String polarisId, long tekkenPower, int danRank, int rating) {
        Optional<Player> playerOptional = playerRepository.findById(userId);
        Player player;

        if (playerOptional.isPresent()) {
            player = playerOptional.get();
        } else {
            player = new Player();
            player.setUserId(userId);
        }

        // Update player's details with the latest information
        player.setName(name);
        player.setPolarisId(polarisId);
        player.setTekkenPower(tekkenPower);
        player.setDanRank(danRank);
        player.setRating(rating);

        return playerRepository.save(player);
    }

    private void updatePlayerWithBattle(Player player, Battle battle) {
        // Update player's last 10 battles list
        List<Battle> last10Battles = player.getLast10Battles();
        if (last10Battles.size() >= 10) {
            last10Battles.remove(0); // Remove the oldest battle if we already have 10
        }
        last10Battles.add(battle);
        player.setLast10Battles(last10Battles);

        // Save the updated player information
        playerRepository.save(player);
    }
}
