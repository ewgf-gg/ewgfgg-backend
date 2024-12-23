package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.tekkenstats.models.TekkenStatsSummary;

import java.util.Optional;

@Repository
public interface TekkenStatsSummaryRepository extends JpaRepository<TekkenStatsSummary, Integer>
{
    @Query(value = "SELECT * FROM tekken_stats_summary", nativeQuery = true)
    Optional<TekkenStatsSummary> getTekkenStatsSummary();

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO tekken_stats_summary (id, total_replays, total_players) " +
            "VALUES (1, 0, 0) " +
            "ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void initializeStatsSummaryTable();

    @Modifying
    @Transactional
    @Query(value = "UPDATE tekken_stats_summary SET total_players = (SELECT COUNT(*) FROM players) WHERE id = 1", nativeQuery = true)
    void updateTotalPlayersCount();

}