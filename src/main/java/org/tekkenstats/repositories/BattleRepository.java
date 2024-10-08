package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tekkenstats.models.Battle;

import java.util.List;
import java.util.Set;

@Repository
public interface BattleRepository extends JpaRepository<Battle, String> {

    List<Battle> findAllByBattleIdIn(Set<String> battleIds);
    Battle findByBattleId(String battleId);
}
