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
                SELECT
                    SUM(total_players) AS total
                FROM aggregated_statistics
                WHERE game_version = :gameVersion
                AND category = :category
            )
            SELECT
                CAST(dan_rank AS int) as rank,
                (SUM(total_players) * 100.0 / (SELECT total FROM total_players)) as percentage
            FROM aggregated_statistics
            WHERE game_version = :gameVersion
            AND category = :category
            GROUP BY dan_rank
            ORDER BY dan_rank
            """, nativeQuery = true)
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

    @Query(value = """
            WITH latest_versions AS (
                SELECT DISTINCT game_version
                FROM aggregated_statistics
                ORDER BY game_version DESC
                LIMIT 2
            ),
            version_winrates AS (
                SELECT
                    a.character_id,
                    a.game_version,
                    CASE
                        WHEN dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                        WHEN dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                        WHEN dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
                    END as rank_category,
                    SUM(a.total_wins) as total_wins,
                    SUM(a.total_losses) as total_losses,
                    ROUND(
                        SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0),
                        2
                    ) as winrate
                FROM aggregated_statistics a
                INNER JOIN latest_versions lv ON a.game_version = lv.game_version
                WHERE a.category = 'standard'
                GROUP BY
                    a.character_id,
                    a.game_version,
                    CASE
                        WHEN dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                        WHEN dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                        WHEN dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
                    END
            ),
            winrate_changes AS (
                SELECT
                    v1.character_id,
                    v1.rank_category,
                    v1.winrate as new_winrate,
                    v2.winrate as old_winrate,
                    (v1.winrate - v2.winrate) as winrate_change
                FROM version_winrates v1
                INNER JOIN version_winrates v2
                    ON v1.character_id = v2.character_id
                    AND v1.game_version > v2.game_version
                    AND v1.rank_category = v2.rank_category
                WHERE v1.winrate IS NOT NULL
                    AND v2.winrate IS NOT NULL
            ),
            top_increases AS (
                SELECT
                    character_id,
                    rank_category,
                    winrate_change,
                    'increase' as trend,
                    ROW_NUMBER() OVER (PARTITION BY rank_category ORDER BY winrate_change DESC) as rn
                FROM winrate_changes
                WHERE winrate_change > 0
            ),
            top_decreases AS (
                SELECT
                    character_id,
                    rank_category,
                    winrate_change,
                    'decrease' as trend,
                    ROW_NUMBER() OVER (PARTITION BY rank_category ORDER BY winrate_change) as rn
                FROM winrate_changes
                WHERE winrate_change < 0
            )
            SELECT
                character_id as characterId,
                rank_category as rankCategory,
                CAST(ABS(winrate_change) AS DOUBLE PRECISION) as change,
                trend
            FROM (
                SELECT * FROM top_increases WHERE rn <= 2
                UNION ALL
                SELECT * FROM top_decreases WHERE rn <= 2
            ) as "ti*td*"
            ORDER BY rank_category DESC
            """, nativeQuery = true)
    List<Object[]> getWinrateChanges();

    @Query(value = "SELECT DISTINCT game_version FROM aggregated_statistics", nativeQuery = true)
    Optional<List<Integer>> getGameVersions();
}
