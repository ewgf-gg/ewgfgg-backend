package org.ewgf.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ewgf.interfaces.BattlesProjection;
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

    @Query(value = "SELECT " +
            "battles.date as date, " +
            "battles.game_version as gameVersion,"+
            "battles.player1_name as player1Name," +
            "battles.player1_polaris_id as player1PolarisId," +
            "battles.player1_character_id as player1CharacterId, " +
            "battles.player1_region as player1RegionId, " +
            "battles.player1_tekken_power as player1TekkenPower," +
            "battles.player1_dan_rank as player1DanRank, " +
            "battles.player2_name as player2Name, " +
            "battles.player2_polaris_id as player2PolarisId, " +
            "battles.player2_character_id as player2CharacterId, " +
            "battles.player2_region as player2RegionId, " +
            "battles.player2_tekken_power as player2TekkenPower, " +
            "battles.player2_dan_rank as player2DanRank, " +
            "battles.player1_rounds_won as player1RoundsWon, " +
            "battles.player2_rounds_won as player2RoundsWon, " +
            "battles.winner as winner, " +
            "battles.stageid as stageId " +
            "FROM battles WHERE player1_id = :playerID OR player2_id = :playerID " +
            "ORDER BY battles.battle_at DESC",
            nativeQuery = true)
    Optional<List<BattlesProjection>> findAllBattlesByPlayer(@Param("playerID") String playerID);

}