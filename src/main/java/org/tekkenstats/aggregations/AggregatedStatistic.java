package org.tekkenstats.aggregations;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "aggregated_statistics")
@Data
public class AggregatedStatistic {

    @EmbeddedId
    private AggregatedStatisticId id;

    @Column(name = "total_wins")
    private int totalWins;

    @Column(name = "total_losses")
    private int totalLosses;

    @Column(name = "total_players")
    private int totalPlayers;

    @Column(name = "total_replays")
    private int totalReplays;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    // Getters and setters


    public AggregatedStatistic(AggregatedStatisticId id)
    {
        this.id = id;
    }

    public AggregatedStatistic() {}
}

