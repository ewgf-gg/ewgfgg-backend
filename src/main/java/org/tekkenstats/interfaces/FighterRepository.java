package org.tekkenstats.interfaces;

import org.tekkenstats.Fighter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FighterRepository extends MongoRepository<Fighter, Integer>
{
}

//