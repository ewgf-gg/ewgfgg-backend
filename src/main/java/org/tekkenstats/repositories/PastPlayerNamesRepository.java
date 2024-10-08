package org.tekkenstats.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tekkenstats.models.PastPlayerNames;

@Repository
public interface PastPlayerNamesRepository extends JpaRepository<PastPlayerNames, String> {
}
