package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.tekkenstats.models.Battle;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BattleRepository extends JpaRepository<Battle, String> {

    @Query("SELECT b FROM Battle b ORDER BY b.battleAt ASC LIMIT 1")
    Optional<Battle> findOldestBattle();

    @Query("SELECT b FROM Battle b ORDER BY b.battleAt DESC LIMIT 1")
    Optional<Battle> findLatestBattle();

}

