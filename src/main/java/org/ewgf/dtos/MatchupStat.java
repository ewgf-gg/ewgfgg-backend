package org.ewgf.dtos;

import lombok.Data;

@Data
public class MatchupStat {
    private Integer wins = 0;
    private Integer losses = 0;
    private Float winRate;
    private Integer totalMatches;

    public void incrementWins() {
        this.wins++;
        this.totalMatches++;
        calculateWinrate();
    }
    public void incrementLosses() {
        this.losses++;
        this.totalMatches++;
        calculateWinrate();
    }

    public void calculateWinrate(){
        this.winRate = totalMatches > 0 ? (float) this.wins / totalMatches : 0f;
    }
}


