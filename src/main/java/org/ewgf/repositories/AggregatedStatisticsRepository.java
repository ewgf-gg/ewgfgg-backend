package org.ewgf.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ewgf.aggregations.AggregatedStatistic;
import org.ewgf.aggregations.AggregatedStatisticId;
import org.ewgf.interfaces.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedStatisticsRepository extends JpaRepository<AggregatedStatistic, AggregatedStatisticId> {

    List<AggregatedStatistic> findByIdGameVersionAndIdCategory(int gameVersion, String category);


    @Query(value = """
    WITH global_all_ranks AS (
        SELECT
            game_version,
            character_id,
            SUM(total_wins) as total_wins,
            SUM(total_losses) as total_losses,
            SUM(total_replays) as total_replays
        FROM aggregated_statistics
        WHERE category = 'standard'
        GROUP BY game_version, character_id
    ),
    global_by_rank AS (
        SELECT
            game_version,
            character_id,
            CASE
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END as rank_category,
            SUM(total_wins) as total_wins,
            SUM(total_losses) as total_losses,
            SUM(total_replays) as total_replays
        FROM aggregated_statistics
        WHERE category = 'standard'
        GROUP BY
            game_version,
            character_id,
            CASE
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END
    )
    SELECT * FROM (
        -- All ranks query with global stats
        SELECT
            game_version as gameVersion,
            'allRanks' as rankCategory,
            'Global' as regionId,
            character_id as characterId,
            total_wins as totalWins,
            total_losses as totalLosses,
            total_replays as totalBattles
        FROM global_all_ranks
    
        UNION ALL
    
        -- All ranks query with regional stats
        SELECT
            a.game_version as gameVersion,
            'allRanks' as rankCategory,
            a.region_id::text as regionId,
            a.character_id as characterId,
            SUM(a.total_wins) as totalWins,
            SUM(a.total_losses) as totalLosses,
            SUM(a.total_replays) as totalBattles
        FROM aggregated_statistics a
        WHERE a.category = 'standard'
        GROUP BY
            a.game_version,
            a.region_id,
            a.character_id
        HAVING SUM(a.total_replays) > 0
    
        UNION ALL
    
        -- Rank categories query with global stats
        SELECT
            game_version as gameVersion,
            rank_category as rankCategory,
            'Global' as regionId,
            character_id as characterId,
            total_wins as totalWins,
            total_losses as totalLosses,
            total_replays as totalBattles
        FROM global_by_rank
    
        UNION ALL
    
        -- Rank categories query with regional stats
        SELECT
            a.game_version as gameVersion,
            CASE
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END as rankCategory,
            a.region_id::text as regionId,
            a.character_id as characterId,
            SUM(a.total_wins) as totalWins,
            SUM(a.total_losses) as totalLosses,
            SUM(a.total_replays) as totalBattles
        FROM aggregated_statistics a
        WHERE a.category = 'standard'
        GROUP BY
            a.game_version,
            CASE
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END,
            a.region_id,
            a.character_id
        HAVING SUM(a.total_replays) > 0
    ) combined
    ORDER BY
        gameVersion DESC,
        CASE WHEN rankCategory = 'allRanks' THEN 0 ELSE 1 END,
        rankCategory,
        CASE WHEN regionId = 'Global' THEN 0 ELSE 1 END,
        regionId,
        totalBattles DESC
    """, nativeQuery = true)
    List<CharacterAnalyticsProjection> findAllCharactersByPopularity();

    @Query(value = """
    WITH global_all_ranks AS (
        SELECT
            game_version,
            character_id,
            SUM(total_wins) as total_wins,
            SUM(total_losses) as total_losses
        FROM aggregated_statistics
        WHERE category = 'standard'
        GROUP BY game_version, character_id
    ),
    global_by_rank AS (
        SELECT
            game_version,
            character_id,
            CASE
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END as rank_category,
            SUM(total_wins) as total_wins,
            SUM(total_losses) as total_losses
        FROM aggregated_statistics
        WHERE category = 'standard'
        GROUP BY
            game_version,
            character_id,
            CASE
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END
    )
    SELECT * FROM (
        -- All ranks query with global stats
        SELECT
            game_version as gameVersion,
            'allRanks' as rankCategory,
            'Global' as regionId,
            character_id as characterId,
            total_wins as totalWins,
            total_losses as totalLosses,
            (total_wins::float / NULLIF((total_wins + total_losses), 0)) * 100 as winrate
        FROM global_all_ranks
        
        UNION ALL
        
        -- All ranks query with regional stats
        SELECT
            a.game_version as gameVersion,
            'allRanks' as rankCategory,
            a.region_id::text as regionId,
            a.character_id as characterId,
            SUM(a.total_wins) as totalWins,
            SUM(a.total_losses) as totalLosses,
            (SUM(a.total_wins)::float / NULLIF(SUM(a.total_wins + a.total_losses), 0)) * 100 as winrate
        FROM aggregated_statistics a
        WHERE a.category = 'standard'
        GROUP BY 
            a.game_version,
            a.region_id,
            a.character_id
        HAVING SUM(a.total_wins + a.total_losses) > 0
        
        UNION ALL
        
        -- Rank categories query with global stats
        SELECT 
            game_version as gameVersion,
            rank_category as rankCategory,
            'Global' as regionId,
            character_id as characterId,
            total_wins as totalWins,
            total_losses as totalLosses,
            (total_wins::float / NULLIF((total_wins + total_losses), 0)) * 100 as winrate
        FROM global_by_rank
        
        UNION ALL
        
        -- Rank categories query with regional stats
        SELECT 
            a.game_version as gameVersion,
            CASE 
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END as rankCategory,
            a.region_id::text as regionId,
            a.character_id as characterId,
            SUM(a.total_wins) as totalWins,
            SUM(a.total_losses) as totalLosses,
            (SUM(a.total_wins)::float / NULLIF(SUM(a.total_wins + a.total_losses), 0)) * 100 as winrate
        FROM aggregated_statistics a
        WHERE a.category = 'standard'
        GROUP BY 
            a.game_version,
            CASE 
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END,
            a.region_id,
            a.character_id
        HAVING SUM(a.total_wins + a.total_losses) > 0
    ) combined
    ORDER BY 
        gameVersion DESC,
        CASE WHEN rankCategory = 'allRanks' THEN 0 ELSE 1 END,
        rankCategory,
        CASE WHEN regionId = 'Global' THEN 0 ELSE 1 END,
        regionId,
        winrate DESC
""", nativeQuery = true)
    List<CharacterWinrateProjection> findAllWinrateStats();

    @Query(value = """
    WITH latest_version AS (
        SELECT MAX(game_version) as version FROM aggregated_statistics
    ),
    ranked_stats AS (
        SELECT 
            CASE 
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END as rankCategory,
            a.character_id as characterId,
            SUM(a.total_replays) as totalBattles,
            ROW_NUMBER() OVER (
                PARTITION BY 
                    CASE 
                        WHEN dan_rank >= 27 THEN 'master'
                        WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                        WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                        WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
                    END 
                ORDER BY SUM(a.total_replays) DESC
            ) as rank
        FROM aggregated_statistics a, latest_version lv
        WHERE a.category = 'standard'
            AND a.game_version = lv.version
        GROUP BY 
            character_id,
            CASE 
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
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
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
            END as rankCategory,
            a.character_id as characterId,
            SUM(a.total_wins) as totalWins,
            SUM(a.total_losses) as totalLosses,
            ROUND(SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0), 2) as winratePercentage,
            ROW_NUMBER() OVER (
                PARTITION BY 
                    CASE 
                        WHEN dan_rank >= 27 THEN 'master'
                        WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                        WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                        WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
                    END 
                ORDER BY SUM(a.total_wins) * 100.0 / NULLIF(SUM(a.total_wins + a.total_losses), 0) DESC
            ) as rank
        FROM aggregated_statistics a, latest_version lv
        WHERE a.category = 'standard'
            AND a.game_version = lv.version
        GROUP BY 
            character_id,
            CASE 
                WHEN dan_rank >= 27 THEN 'master'
                WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
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
                    WHEN dan_rank >= 27 THEN 'master'
                    WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                    WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                    WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
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
                    WHEN dan_rank >= 27 THEN 'master'
                    WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                    WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                    WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
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
                    WHEN dan_rank >= 27 THEN 'master'
                    WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
                    WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
                    WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
                    ELSE NULL -- Handle any potential NULL dan_ranks
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
    WHEN dan_rank >= 27 THEN 'master'
    WHEN dan_rank BETWEEN 21 AND 26 THEN 'advanced'
    WHEN dan_rank BETWEEN 15 AND 20 THEN 'intermediate'
    WHEN dan_rank BETWEEN 0 AND 14 THEN 'beginner'
    END

    UNION ALL

    -- Global tier that includes all ranks
    SELECT
    a.character_id,
    a.game_version,
            'global' as rank_category,
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
    a.game_version
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
            SELECT * FROM top_increases
            UNION ALL
            SELECT * FROM top_decreases
    ) as combined_results
    ORDER BY
    CASE
    WHEN rank_category = 'global' THEN 1
    WHEN rank_category = 'master' THEN 2
    WHEN rank_category = 'advanced' THEN 3
    WHEN rank_category = 'intermediate' THEN 4
    WHEN rank_category = 'beginner' THEN 5
    END""", nativeQuery = true)
    List<WinrateChangesProjection> getAllWinrateChanges();


    @Query(value = "SELECT DISTINCT game_version FROM aggregated_statistics", nativeQuery = true)
    Optional<List<Integer>> getGameVersions();
}
