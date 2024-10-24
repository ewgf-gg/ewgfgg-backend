package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.aggregations.AggregatedStatistic;
import org.tekkenstats.aggregations.AggregatedStatisticId;
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

    @Query(value = "SELECT DISTINCT game_version FROM aggregated_statistics", nativeQuery = true)
    Optional<List<Integer>> getGameVersions();

}
