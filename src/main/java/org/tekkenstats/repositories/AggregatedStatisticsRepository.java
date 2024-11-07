package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.aggregations.AggregatedStatistic;
import org.tekkenstats.aggregations.AggregatedStatisticId;
import org.tekkenstats.interfaces.CharacterWinrateProjection;
import org.tekkenstats.interfaces.RankDistributionProjection;
import org.tekkenstats.mappers.enumsMapper;

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

        WITH latest_version AS (
        SELECT MAX(game_version) as version
        FROM aggregated_statistics
    ),
    max_rank AS (
        SELECT MAX(dan_rank) as highest_rank
        FROM aggregated_statistics a
        INNER JOIN latest_version lv ON a.game_version = lv.version
        WHERE a.category = 'standard'
    ),
    top_ranks AS (
        SELECT DISTINCT dan_rank
        FROM aggregated_statistics a, max_rank mr
        WHERE dan_rank >= (mr.highest_rank - 4)
        AND dan_rank <= mr.highest_rank
    )
    SELECT\s
        a.character_id as characterId,
        SUM(a.total_wins) as totalWins,
        SUM(a.total_losses) as totalLosses,
        ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
    FROM aggregated_statistics a
    INNER JOIN latest_version lv ON a.game_version = lv.version
    INNER JOIN top_ranks tr ON a.dan_rank = tr.dan_rank
    WHERE a.category = 'standard'
    GROUP BY a.character_id
    HAVING SUM(a.total_wins + a.total_losses) > 0
    ORDER BY winratePercentage DESC
    LIMIT 5
    """, nativeQuery = true)
    List<CharacterWinrateProjection> findTop5CharactersByWinrateInStandard();


    @Query(value = """
    WITH latest_version AS (
        SELECT MAX(game_version) as version
        FROM aggregated_statistics
    ),
    low_ranks AS (
        SELECT DISTINCT dan_rank
        FROM aggregated_statistics
        WHERE dan_rank BETWEEN 0 AND 23
    )
    SELECT 
        a.character_id as characterId,
        SUM(a.total_wins) as totalWins,
        SUM(a.total_losses) as totalLosses,
        ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage
    FROM aggregated_statistics a
    INNER JOIN latest_version lv ON a.game_version = lv.version
    INNER JOIN low_ranks lr ON a.dan_rank = lr.dan_rank
    WHERE a.category = 'standard'
    GROUP BY a.character_id
    HAVING SUM(a.total_wins + a.total_losses) > 0
    ORDER BY winratePercentage DESC
    LIMIT 5
    """, nativeQuery = true)
    List<CharacterWinrateProjection> findTop5CharactersByWinrateInLowRanks();

    @Query(value = "SELECT DISTINCT game_version FROM aggregated_statistics", nativeQuery = true)
    Optional<List<Integer>> getGameVersions();

}
