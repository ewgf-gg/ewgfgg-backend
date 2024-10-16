package org.tekkenstats.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "character_stats")
@Data
public class CharacterStats {

    @EmbeddedId
    private CharacterStatsId id;

    @Column(name = "latest_battle")
    private long latestBattle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    @Column(name = "wins")
    private int wins;

    @Column(name = "losses")
    private int losses;

    @Column(name = "dan_rank")
    private int danRank;

    @Transient
    private int winsIncrement = 0;

    @Transient
    private int lossIncrement = 0;

    @Transient
    private int ratingChange = 0;

    public CharacterStats() {
        this.wins = 0;
        this.losses = 0;
        this.danRank = 0;
        this.latestBattle = 0;
    }

    public CharacterStats(int wins, int losses, int danRank, long latestBattle) {
        this.wins = wins;
        this.losses = losses;
        this.danRank = danRank;
        this.latestBattle = latestBattle;
    }
}
