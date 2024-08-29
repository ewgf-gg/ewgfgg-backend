package org.tekkenstats.interfaces;
import org.tekkenstats.EnumDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface EnumRepository extends MongoRepository<EnumDocument, String> {
    @Query(value = "{'fighters': {$exists: true}}")
    EnumDocument findFightersDocument();
}