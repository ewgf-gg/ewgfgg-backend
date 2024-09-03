package org.tekkenstats.interfaces;
import org.tekkenstats.EnumDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface EnumRepository extends MongoRepository<EnumDocument, String> {
    @Query(value = "{'fighters': {$exists: true}}")
    EnumDocument findFightersDocument();

    @Query(value = "{'dan_names': {$exists:true}}")
    EnumDocument findDanNamesDocument();

    @Query(value = "{'battle_type': {$exists:true}}")
    EnumDocument findBattleTypeDocument();

    @Query(value = "{'stages': {$exists:true}}")
    EnumDocument findStagesDocument();

    @Query(value = "{'platform': {$exists:true}}")
    EnumDocument findPlatformDocument();

}