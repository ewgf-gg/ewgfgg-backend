package org.tekkenstats.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tekkenstats.models.Player;

import java.util.Optional;


@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    @Query(value = "SELECT * FROM players p WHERE p.id = :criteria OR p.name ILIKE :criteria OR p.polaris_id ILIKE :criteria", nativeQuery = true)
    Optional<Player> findByIdOrNameOrPolarisIdIgnoreCase(@Param("criteria") String criteria);

    @Query(value = "SELECT COUNT (*) FROM players", nativeQuery=true)
    Optional<Long> getPlayerCount();

}

