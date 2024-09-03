package org.tekkenstats.interfaces;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.tekkenstats.Player;
import org.tekkenstats.mdbDocuments.PlayerDocument;

import java.util.List;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {

    // Custom query to find players by name
    @Query(value = "{ 'name': ?0 }")
    List<Player> findByName(String name);

    @Query(value = "{'user_id: ?0} ")
    PlayerDocument findPlayerByID();
}
