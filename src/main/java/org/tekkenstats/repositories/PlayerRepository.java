package org.tekkenstats.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.Player;

import java.util.List;
import java.util.Set;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    List<Player> findAllByPlayerIdIn(Set<String> playerIDs);
    Player findByPlayerId(String playerID);

    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO players (user_id, name, polaris_id, tekken_power, latest_battle, version) " +
                    "VALUES (:#{#player.userId}, :#{#player.name}, :#{#player.polarisId}, :#{#player.tekkenPower}, :#{#player.latestBattle}, :#{#player.version}) " +
                    "ON CONFLICT (user_id) DO UPDATE SET " +
                    "name = EXCLUDED.name, " +
                    "polaris_id = EXCLUDED.polaris_id, " +
                    "tekken_power = EXCLUDED.tekken_power, " +
                    "latest_battle = GREATEST(players.latest_battle, EXCLUDED.latest_battle), " +
                    "version = players.version + 1",
            nativeQuery = true
    )
    void upsertPlayer(@Param("player") Player player);
}

