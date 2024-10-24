package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.tekkenstats.models.TekkenStatsSummary;

import java.util.Optional;

@Repository
public interface TekkenStatsSummaryRepository extends JpaRepository<TekkenStatsSummary, Integer>
{
    @Query(value = "SELECT * FROM tekken_stats_summary", nativeQuery = true)
    Optional<TekkenStatsSummary> getTekkenStatsSummary();
}