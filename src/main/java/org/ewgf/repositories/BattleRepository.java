package org.ewgf.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ewgf.models.Battle;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BattleRepository extends JpaRepository<Battle, String> {

    @Query("SELECT b FROM Battle b ORDER BY b.battleAt ASC LIMIT 1")
    Optional<Battle> findOldestBattle();

    @Query("SELECT b FROM Battle b ORDER BY b.battleAt DESC LIMIT 1")
    Optional<Battle> findNewestBattle();

    @Query(value = "SELECT battle_id FROM (" +
            "(SELECT battle_id FROM battles WHERE battle_at <= :timestamp ORDER BY battle_at DESC LIMIT 40000) " +
            "UNION " +
            "(SELECT battle_id FROM battles WHERE battle_at > :timestamp ORDER BY battle_at ASC LIMIT 40000)" +
            ") AS combined_battles", nativeQuery = true)
    Set<String> findSurroundingBattleIds(@Param("timestamp") long timestamp);

    @Query(value = "SELECT * FROM battles WHERE player1_id = :playerId OR player2_id = :playerId " +
            "ORDER BY battles.battle_at DESC",
            nativeQuery = true)
    Optional<List<Battle>> findAllBattlesByPlayer(@Param("playerId") String playerId);

    @Query(value = "SELECT * FROM battles " +
            "WHERE player1_id = :playerId OR player2_id = :playerId " +
            "ORDER BY battles.battle_at DESC",
            nativeQuery = true)
    Optional<List<Battle>> findAllBattlesByPlayerId(@Param("playerId") String playerId);

}