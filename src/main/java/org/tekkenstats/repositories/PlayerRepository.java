package org.tekkenstats.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import org.tekkenstats.models.Player;



@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

}

