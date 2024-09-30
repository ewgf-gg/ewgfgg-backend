package org.tekkenstats.repositories;

import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.Player;

import java.util.List;
import java.util.Set;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {


    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.playerNames WHERE p.playerId IN :playerIds")
    List<Player> findAllByPlayerIdIn(@Param("playerIds") Set<String> playerIds);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Player p WHERE p.playerId = :playerId")
    Player findByIdForUpdate(@Param("playerId") String playerId);


    @Modifying
    @Query(
            value = "INSERT INTO players (user_id, name, polaris_id, tekken_power, latest_battle) " +
                    "VALUES (:#{#player.playerId}, :#{#player.name}, :#{#player.polarisId}, :#{#player.tekkenPower}, :#{#player.latestBattle}) " +
                    "ON CONFLICT (user_id) DO UPDATE SET " +
                    "name = EXCLUDED.name, " +
                    "polaris_id = EXCLUDED.polaris_id, " +
                    "tekken_power = EXCLUDED.tekken_power, " +
                    "latest_battle = GREATEST(players.latest_battle, EXCLUDED.latest_battle)",
            nativeQuery = true
    )
    void upsertPlayer(@Param("player") Player player);

}

