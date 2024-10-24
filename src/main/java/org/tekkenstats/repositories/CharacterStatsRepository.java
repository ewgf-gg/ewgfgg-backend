package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.models.CharacterStatsId;
import org.tekkenstats.models.CharacterStats;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterStatsRepository extends JpaRepository<CharacterStats, CharacterStatsId>
{
    @Query("SELECT DISTINCT c.id.gameVersion FROM CharacterStats c")
    Optional<List<Integer>> findAllGameVersions();

    @Query("SELECT cs.id.playerId, cs.id.characterId, cs.danRank, cs.wins, cs.losses " +
            "FROM CharacterStats cs WHERE cs.id.gameVersion = :gameVersion")
    List<Object[]> findAllStatsByGameVersion(@Param("gameVersion") int gameVersion);

    @Query("SELECT COUNT(DISTINCT cs.id.playerId) FROM CharacterStats cs WHERE cs.id.gameVersion = :gameVersion")
    int countDistinctPlayersByGameVersion(@Param("gameVersion") int gameVersion);

    @Query("SELECT SUM(cs.wins + cs.losses) FROM CharacterStats cs WHERE cs.id.gameVersion = :gameVersion")
    int sumTotalReplaysByGameVersion(@Param("gameVersion") int gameVersion);
}
