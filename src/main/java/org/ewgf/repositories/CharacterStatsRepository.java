package org.ewgf.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ewgf.models.CharacterStatsId;
import org.ewgf.models.CharacterStats;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterStatsRepository extends JpaRepository<CharacterStats, CharacterStatsId>
{
    @Query("SELECT DISTINCT c.id.gameVersion FROM CharacterStats c")
    Optional<List<Integer>> findAllGameVersions();

    @Query(value = """
        SELECT
            cs.player_id as playerId,
            cs.character_id as characterId,
            cs.dan_rank as danRank,
            cs.wins as wins,
            cs.losses as losses,
            p.region_id as regionId,
            p.area_id as areaId
        FROM character_stats cs
        JOIN players p ON cs.player_id = p.player_id
        WHERE cs.game_version = :gameVersion
        AND p.region_id IS NOT NULL
        AND p.area_id IS NOT NULL
        """,
            nativeQuery = true)
    List<Object[]> findAllStatsByGameVersion(@Param("gameVersion") int gameVersion);
}
