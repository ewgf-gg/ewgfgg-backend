package org.tekkenstats.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.interfaces.PlayerWithBattlesProjection;
import org.tekkenstats.models.Player;

import java.util.List;
import java.util.Optional;


@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    @Query(value = "SELECT * FROM players p WHERE p.player_id = :criteria OR p.name ILIKE :criteria OR p.polaris_id ILIKE :criteria", nativeQuery = true)
    Optional<Player> findByIdOrNameOrPolarisIdIgnoreCase(@Param("criteria") String criteria);

    @Query(value = """
            WITH player_data AS (
                SELECT * FROM players WHERE polaris_id = :criteria
            ),
            character_stats_data AS (
                SELECT 
                    cs.player_id,
                    cs.character_id,
                    cs.game_version,
                    cs.dan_rank,
                    cs.wins,
                    cs.losses,
                    cs.latest_battle
                FROM character_stats cs
                JOIN player_data p ON cs.player_id = p.player_id
            ),
            battle_data AS (
                SELECT 
                    b.date,
                    b.player1_name,
                    b.player1_character_id,
                    b.player1_region,
                    b.player1_dan_rank,
                    b.player2_name,
                    b.player2_region,
                    b.player2_character_id,
                    b.player2_dan_rank,
                    b.player1_rounds_won,
                    b.player2_rounds_won,
                    b.winner,
                    b.stageid
                FROM battles b
                JOIN player_data p ON b.player1_id = p.player_id OR b.player2_id = p.player_id
                ORDER BY b.battle_at DESC
            )
            SELECT 
                p.player_id as playerId,
                p.name as name,
                p.polaris_id as polarisId,
                p.tekken_power as tekkenPower,
                p.region_id as regionId,
                p.area_id as areaId,
                p.language as language,
                p.latest_battle as latestBattle,
                cs.character_id as characterId,
                cs.game_version as gameVersion,
                cs.dan_rank as danRank,
                cs.wins as wins,
                cs.losses as losses,
                cs.latest_battle as characterLatestBattle,
                b.date as date,
                b.player1_name as player1Name,
                b.player1_character_id as player1CharacterId,
                b.player1_region as player1RegionId,
                b.player1_dan_rank as player1DanRank,
                b.player2_name as player2Name,
                b.player2_region as player2RegionId,
                b.player2_character_id as player2CharacterId,
                b.player2_dan_rank as player2DanRank,
                b.player1_rounds_won as player1RoundsWon,
                b.player2_rounds_won as player2RoundsWon,
                b.winner as winner,
                b.stageid as stageId
            FROM player_data p
            LEFT JOIN character_stats_data cs ON 1=1
            LEFT JOIN battle_data b ON 1=1
            """,
            nativeQuery = true)
    List<PlayerWithBattlesProjection> findPlayerWithBattlesAndStats(@Param("criteria") String criteria);


    @Query(value = """
    SELECT * FROM players
    WHERE LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
    OR LOWER(polaris_id) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY
        CASE
            WHEN LOWER(name) = LOWER(:query) THEN 0
            WHEN LOWER(name) LIKE LOWER(CONCAT(:query, '%')) THEN 1
            ELSE 20
        END,
        length(name)
    LIMIT 20
    """, nativeQuery = true)
    Optional<List<Player>> findByNameOrPolarisIdContainingIgnoreCase(@Param("query") String query);

    @Query(value = "SELECT COUNT (*) FROM players", nativeQuery=true)
    Optional<Long> getPlayerCount();

}

