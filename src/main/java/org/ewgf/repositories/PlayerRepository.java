package org.ewgf.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ewgf.models.Player;

import java.util.List;
import java.util.Optional;


@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    @Query("SELECT p FROM Player p WHERE p.polarisId = :criteria")
    Optional<Player> findByPolarisId(@Param("criteria") String criteria);

    @Query("SELECT p FROM Player p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.polarisId) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY CASE " +
            "  WHEN LOWER(p.name) = LOWER(:query) THEN 0 " +
            "  WHEN LOWER(p.name) LIKE LOWER(CONCAT(:query, '%')) THEN 1 " +
            "  ELSE 20 END, " +
            "length(p.name)")
    Optional<List<Player>> findByNameOrPolarisIdContainingIgnoreCase(@Param("query") String query);

    // 600 is in seconds
    @Query(value = "SELECT * FROM players " +
            "WHERE latest_battle > (EXTRACT(EPOCH FROM NOW()) - 600) ORDER BY latest_battle DESC LIMIT 40", nativeQuery = true)
    Optional<List<Player>> findAllActivePlayersInLast10Minutes();
}