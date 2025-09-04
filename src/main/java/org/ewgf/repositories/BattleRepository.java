package org.ewgf.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ewgf.models.Battle;

import java.util.List;
import java.util.Optional;

@Repository
public interface BattleRepository extends JpaRepository<Battle, String> {

    @Query(value = "SELECT * FROM battles WHERE battle_type = '2' ORDER BY battle_at ASC LIMIT 1",
            nativeQuery = true)
    Optional<Battle> findOldestRankedBattle();

    @Query(value = "SELECT * FROM battles WHERE battle_type = '2' ORDER BY battle_at DESC LIMIT 1",
            nativeQuery = true)
    Optional<Battle> findNewestRankedBattle();

    @Query(value = "SELECT * FROM battles WHERE " +
            "(player1_id = :playerId OR player1_id = LPAD(:playerId, 18, '0')) OR " +
            "(player2_id = :playerId OR player2_id = LPAD(:playerId, 18, '0')) " +
            "ORDER BY battles.battle_at DESC",
            nativeQuery = true)
    Optional<List<Battle>> findAllBattlesByPlayer(@Param("playerId") String playerId);

    @Query(value = "SELECT * FROM battles " +
            "WHERE player1_id = :playerId OR player2_id = :playerId " +
            "ORDER BY battles.battle_at DESC",
            nativeQuery = true)
    Optional<List<Battle>> findAllBattlesByPlayerId(@Param("playerId") String playerId);

}