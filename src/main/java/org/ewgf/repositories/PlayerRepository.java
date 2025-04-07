package org.ewgf.repositories;


import org.ewgf.dtos.homepage.RegionalPlayerDistributionDTO;
import org.ewgf.interfaces.RegionalPlayerDistributionProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ewgf.models.Player;

import java.util.List;
import java.util.Optional;


@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    @Query(value = "SELECT * FROM players p WHERE p.player_id = :criteria OR p.name ILIKE :criteria OR p.polaris_id ILIKE :criteria", nativeQuery = true)
    Optional<Player> findByIdOrNameOrPolarisIdIgnoreCase(@Param("criteria") String criteria);

   @Query(value = "SELECT * FROM players p WHERE p.polaris_id = :criteria",nativeQuery = true)
    Optional<Player> findByPolarisId(@Param("criteria") String criteria);

    @Query(value = """
    SELECT * FROM players
    WHERE LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
    OR LOWER(polaris_id) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY
        CASE
            WHEN LOWER(name) = LOWER(:query) THEN 0
            WHEN LOWER(name) LIKE LOWER(CONCAT(:query, '%')) THEN 1
            ELSE 20
        END,
        length(name)
    LIMIT 50
    """, nativeQuery = true)
    Optional<List<Player>> findByNameOrPolarisIdContainingIgnoreCase(@Param("query") String query);

    // 600 is in seconds
    @Query(value = "SELECT * FROM players " +
            "WHERE latest_battle > (EXTRACT(EPOCH FROM NOW()) - 600) ORDER BY latest_battle DESC LIMIT 40", nativeQuery = true)
    Optional<List<Player>> findAllActivePlayersInLast10Minutes();

    @Query(value = """ 
    SELECT
        SUM(CASE WHEN region_id = '0' THEN 1 ELSE 0 END)::numeric / COUNT(*)::numeric * 100 as Asia,
        SUM(CASE WHEN region_id = '1' THEN 1 ELSE 0 END)::numeric / COUNT(*)::numeric * 100 as MiddleEast,
        SUM(CASE WHEN region_id = '2' THEN 1 ELSE 0 END)::numeric / COUNT(*)::numeric * 100 as Oceania,
        SUM(CASE WHEN region_id = '3' THEN 1 ELSE 0 END)::numeric / COUNT(*)::numeric * 100 as Americas,
        SUM(CASE WHEN region_id = '4' THEN 1 ELSE 0 END)::numeric / COUNT(*)::numeric * 100 as Europe,
        SUM(CASE WHEN region_id IS NULL THEN 1 ELSE 0 END)::numeric / COUNT(*)::numeric * 100 as Unassigned
    FROM players
    """, nativeQuery = true)
    RegionalPlayerDistributionProjection findAllPlayerCountByRegion();
}