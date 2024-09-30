package org.tekkenstats;

import jakarta.persistence.*;
import lombok.Data;

@Embeddable
@Data
public class CharacterStats {

    @Column(name = "latest_battle")
    private long latestBattle;

    @Column(name = "wins")
    private int wins;

    @Column(name = "losses")
    private int losses;

    @Column(name = "dan_rank")
    private int danRank;

    @Column(name = "rating")
    private int rating;

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
        this.rating = 0;
        this.latestBattle = 0;
    }

    public CharacterStats(int wins, int losses, int danRank, int rating, long latestBattle) {
        this.wins = wins;
        this.losses = losses;
        this.danRank = danRank;
        this.rating = rating;
        this.latestBattle = latestBattle;
    }
}
