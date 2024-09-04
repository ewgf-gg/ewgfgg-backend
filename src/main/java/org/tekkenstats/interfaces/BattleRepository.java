package org.tekkenstats.interfaces;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.tekkenstats.Battle;


@Repository
public interface BattleRepository extends MongoRepository<Battle, String> {

    @Query("{ }")
    Battle FindLatestBattle();

}