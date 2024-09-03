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
    List<Player> findByName(String name);

    // Corrected Query Syntax
    @Query(value = "{ 'userId' : ?0 }")  // Use correct field name, no colon in the query key
    PlayerDocument findPlayerByID(String userId);  // Add parameter and use appropriate return type
}
