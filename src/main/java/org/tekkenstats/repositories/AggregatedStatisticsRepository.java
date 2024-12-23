package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.aggregations.AggregatedStatistic;
import org.tekkenstats.aggregations.AggregatedStatisticId;
import org.tekkenstats.interfaces.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedStatisticsRepository extends JpaRepository<AggregatedStatistic, AggregatedStatisticId> {

    List<AggregatedStatistic> findByIdGameVersionAndIdCategory(int gameVersion, String category);


    @Query(value = """
    SELECT 
        a.game_version as gameVersion,
        CASE 
            WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
            WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
            WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
        END as rankCategory,
        COALESCE(region_id::text, 'Global') as regionId,
        a.character_id as characterId,
        SUM(a.total_wins) as totalWins,
        SUM(a.total_losses) as totalLosses,
        SUM(a.total_replays) as totalBattles
    FROM aggregated_statistics a
    WHERE a.category = 'standard'
    GROUP BY GROUPING SETS (
        (game_version, region_id, character_id, 
         CASE 
            WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
            WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
            WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
         END),
        (game_version, character_id,
         CASE 
            WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
            WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
            WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
         END)
    )
    HAVING SUM(a.total_replays) > 0
    ORDER BY 
        game_version DESC,
        rankCategory,
        CASE WHEN region_id IS NULL THEN 0 ELSE 1 END,
        region_id,
        totalBattles DESC
""", nativeQuery = true)
    List<CharacterAnalyticsProjection> findAllCharactersByPopularity();

    @Query(value = """
    SELECT 
        a.game_version as gameVersion,
        CASE 
            WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
            WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
            WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
        END as rankCategory,
        COALESCE(region_id::text, 'Global') as regionId,
        a.character_id as characterId,
        SUM(a.total_wins) as totalWins,
        SUM(a.total_losses) as totalLosses
    FROM aggregated_statistics a
    WHERE a.category = 'standard'
    GROUP BY GROUPING SETS (
        (game_version, region_id, character_id, 
         CASE 
            WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
            WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
            WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
         END),
        (game_version, character_id,
         CASE 
            WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
            WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
            WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
         END)
    )
    HAVING SUM(a.total_wins + a.total_losses) > 0
    ORDER BY 
        game_version DESC,
        rankCategory,
        CASE WHEN region_id IS NULL THEN 0 ELSE 1 END,
        region_id,
        SUM(a.total_wins)::float / NULLIF(SUM(a.total_wins + a.total_losses), 0) DESC
""", nativeQuery = true)
    List<CharacterWinrateProjection> findAllWinrateStats();

    @Query(value = """
    WITH latest_version AS (
        SELECT MAX(game_version) as version FROM aggregated_statistics
    ),
    ranked_stats AS (
        SELECT 
            CASE 
                WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
            END as rankCategory,
            a.character_id as characterId,
            SUM(a.total_replays) as totalBattles,
            ROW_NUMBER() OVER (
                PARTITION BY 
                    CASE 
                        WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                        WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                        WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
                    END 
                ORDER BY SUM(a.total_replays) DESC
            ) as rank
        FROM aggregated_statistics a, latest_version lv
        WHERE a.category = 'standard'
            AND a.game_version = lv.version
        GROUP BY 
            character_id,
            CASE 
                WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
            END
        HAVING SUM(a.total_replays) > 0
    )
    SELECT *
    FROM ranked_stats
    WHERE rank <= 5
    ORDER BY 
        rankCategory,
        totalBattles DESC
""", nativeQuery = true)
    List<CharacterAnalyticsProjection> findTopCharactersByPopularity();

    @Query(value = """
    WITH latest_version AS (
        SELECT MAX(game_version) as version FROM aggregated_statistics
    ),
    ranked_stats AS (
        SELECT 
            CASE 
                WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
            END as rankCategory,
            a.character_id as characterId,
            SUM(a.total_wins) as totalWins,
            SUM(a.total_losses) as totalLosses,
            ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage,
            ROW_NUMBER() OVER (
                PARTITION BY 
                    CASE 
                        WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                        WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                        WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
                    END 
                ORDER BY SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0) DESC
            ) as rank
        FROM aggregated_statistics a, latest_version lv
        WHERE a.category = 'standard'
            AND a.game_version = lv.version
        GROUP BY 
            character_id,
            CASE 
                WHEN a.dan_rank BETWEEN 25 AND 29 THEN 'highRank'
                WHEN a.dan_rank BETWEEN 15 AND 24 THEN 'mediumRank'
                WHEN a.dan_rank BETWEEN 0 AND 14 THEN 'lowRank'
            END
        HAVING SUM(a.total_wins + a.total_losses) > 0
    )
    SELECT *
    FROM ranked_stats
    WHERE rank <= 5
    ORDER BY 
        rankCategory,
        winratePercentage DESC
""", nativeQuery = true)
    List<CharacterAnalyticsProjection> findTopCharactersByWinrate();

    @Query(value = """
        WITH total_players_by_version AS (
            SELECT
                game_version,
                category,
                SUM(total_players) AS total
            FROM aggregated_statistics
            WHERE game_version IN (:gameVersions)
            AND category IN ('overall', 'standard')
            GROUP BY game_version, category
        )
        SELECT
            a.game_version,
            a.category,
            CAST(a.dan_rank AS int) as rank,
            (SUM(a.total_players) * 100.0 / tp.total) as percentage
        FROM aggregated_statistics a
        JOIN total_players_by_version tp 
            ON a.game_version = tp.game_version 
            AND a.category = tp.category
        WHERE a.game_version IN (:gameVersions)
        AND a.category IN ('overall', 'standard')
        GROUP BY a.game_version, a.category, a.dan_rank, tp.total
        ORDER BY a.game_version, a.category, a.dan_rank
        """, nativeQuery = true)
    List<RankDistributionProjection> getAllRankDistributions(@Param("gameVersions") List<Integer> gameVersions);

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
        ) as combined_results
        ORDER BY rank_category DESC
        """,
            nativeQuery = true)
    List<WinrateChangesProjection> getWinrateChanges();


    @Query(value = "SELECT DISTINCT game_version FROM aggregated_statistics", nativeQuery = true)
    Optional<List<Integer>> getGameVersions();
}
