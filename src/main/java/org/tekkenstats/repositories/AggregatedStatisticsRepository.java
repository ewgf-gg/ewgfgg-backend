package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.aggregations.AggregatedStatistic;
import org.tekkenstats.aggregations.AggregatedStatisticId;
import org.tekkenstats.interfaces.CharacterWinrateProjection;
import org.tekkenstats.interfaces.PopularCharacterProjection;
import org.tekkenstats.interfaces.RankDistributionProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedStatisticsRepository extends JpaRepository<AggregatedStatistic, AggregatedStatisticId> {

    List<AggregatedStatistic> findByIdGameVersionAndIdCategory(int gameVersion, String category);

    @Query("SELECT COUNT(p) FROM Player p")
    long countPlayers();

    @Query(value = """
            WITH total_players AS (
                SELECT SUM(total_players) AS total
                FROM aggregated_statistics
                WHERE game_version = :gameVersion AND category = :category
            )
            SELECT
                CAST(dan_rank AS int) as rank,
                (SUM(total_players) * 100.0 / (SELECT total FROM total_players)) as percentage
            FROM aggregated_statistics
            WHERE game_version = :gameVersion AND category = :category
            GROUP BY dan_rank
            ORDER BY dan_rank
            """,
            nativeQuery = true)
    List<RankDistributionProjection> getRankDistribution(@Param("gameVersion") int gameVersion, @Param("category") String category);


    @Query(value = """
    SELECT
    a.character_id as characterId,
    SUM(a.total_wins) as totalWins,
    SUM(a.total_losses) as totalLosses,
    ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
    FROM aggregated_statistics a
    INNER JOIN (
    SELECT MAX(game_version) as version
    FROM aggregated_statistics
    ) lv ON a.game_version = lv.version
    WHERE a.category = 'standard'
    AND a.dan_rank BETWEEN 25 AND 29 --Tekken God and above
    GROUP BY a.character_id
    HAVING SUM(a.total_wins + a.total_losses) > 0
    ORDER BY winratePercentage DESC
    LIMIT 5
    """, nativeQuery = true)
    List<CharacterWinrateProjection> findTop5CharactersByWinrateInHighRank();

    @Query(value = """
    SELECT
    a.character_id as characterId,
    SUM(a.total_wins) as totalWins,
    SUM(a.total_losses) as totalLosses,
    ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
    FROM aggregated_statistics a
    INNER JOIN (
    SELECT MAX(game_version) as version
    FROM aggregated_statistics
    ) lv ON a.game_version = lv.version
    WHERE a.category = 'standard'
    AND a.dan_rank BETWEEN 15 AND 24 --Between Garyu - Bushin
    GROUP BY a.character_id
    HAVING SUM(a.total_wins + a.total_losses) > 0
    ORDER BY winratePercentage DESC
    LIMIT 5
    """, nativeQuery = true)
    List<CharacterWinrateProjection> findTop5CharactersByWinrateInMediumRanks();

    @Query(value = """
    SELECT
    a.character_id as characterId,
    SUM(a.total_wins) as totalWins,
    SUM(a.total_losses) as totalLosses,
    ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
    FROM aggregated_statistics a
    INNER JOIN (
    SELECT MAX(game_version) as version
    FROM aggregated_statistics
    ) lv ON a.game_version = lv.version
    WHERE a.category = 'standard'
    AND a.dan_rank BETWEEN 0 AND 14 --Beginner to Eliminator
    GROUP BY a.character_id
    HAVING SUM(a.total_wins + a.total_losses) > 0
    ORDER BY winratePercentage DESC
    LIMIT 5
    """, nativeQuery = true)
    List<CharacterWinrateProjection> findTop5CharactersByWinrateInLowRanks();


    @Query(value = """
    SELECT
        a.character_id as characterId,
        SUM(a.total_wins) as totalWins,
        SUM(a.total_losses) as totalLosses,
        SUM(a.total_wins + a.total_losses) as totalBattles,
        ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
    FROM aggregated_statistics a
    INNER JOIN (
        SELECT MAX(game_version) as version
        FROM aggregated_statistics
    ) lv ON a.game_version = lv.version
    WHERE a.category = 'standard'
    AND a.dan_rank BETWEEN 25 AND 29
    GROUP BY a.character_id
    HAVING SUM(a.total_wins + a.total_losses) > 0
    ORDER BY totalBattles DESC
    LIMIT 5
    """, nativeQuery = true)
    List<PopularCharacterProjection> findPopularCharactersInHighRanks();


    @Query(value = """
SELECT
    a.character_id as characterId,
    SUM(a.total_wins) as totalWins,
    SUM(a.total_losses) as totalLosses,
    SUM(a.total_wins + a.total_losses) as totalBattles,
    ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
FROM aggregated_statistics a
INNER JOIN (
    SELECT MAX(game_version) as version
    FROM aggregated_statistics
) lv ON a.game_version = lv.version
WHERE a.category = 'standard'
AND a.dan_rank BETWEEN 15 AND 24
GROUP BY a.character_id
HAVING SUM(a.total_wins + a.total_losses) > 0
ORDER BY totalBattles DESC
LIMIT 5
""", nativeQuery = true)
    List<PopularCharacterProjection> findPopularCharactersInMediumRanks();

    @Query(value = """
SELECT
    a.character_id as characterId,
    SUM(a.total_wins) as totalWins,
    SUM(a.total_losses) as totalLosses,
    SUM(a.total_wins + a.total_losses) as totalBattles,
    ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
FROM aggregated_statistics a
INNER JOIN (
    SELECT MAX(game_version) as version
    FROM aggregated_statistics
) lv ON a.game_version = lv.version
WHERE a.category = 'standard'
AND a.dan_rank BETWEEN 0 AND 14
GROUP BY a.character_id
HAVING SUM(a.total_wins + a.total_losses) > 0
ORDER BY totalBattles DESC
LIMIT 5
""", nativeQuery = true)
    List<PopularCharacterProjection> findPopularCharactersInLowRanks();

    @Query(value = "SELECT DISTINCT game_version FROM aggregated_statistics", nativeQuery = true)
    Optional<List<Integer>> getGameVersions();

}
