package org.ewgf.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BatchExecutorService {
    private final JdbcTemplate jdbcTemplate;

    public BatchExecutorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeBatchUpdate(List<Object[]> batchUpdates) {
        String sql = """
            INSERT INTO character_stats (player_id, character_id, dan_rank, game_version, wins, losses)
            VALUES (?, ?, ?, ?, ?,?)
            ON CONFLICT (player_id, character_id, game_version)
            DO UPDATE SET
                wins = EXCLUDED.wins,
                losses = EXCLUDED.losses
            """;

        jdbcTemplate.batchUpdate(sql, batchUpdates);
    }
}
