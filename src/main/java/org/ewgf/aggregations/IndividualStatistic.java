package org.ewgf.aggregations;

import lombok.Data;

@Data
public class IndividualStatistic {
    private String characterId;
    private int danRank;
    private int wins;
    private int losses;
    private int totalPlays;
    private int gameVersion;
    private Integer regionId;

    public IndividualStatistic(String characterId, int danRank, int wins, int losses, int totalPlays, int regionId)
    {
        this.characterId = characterId;
        this.danRank = danRank;
        this.wins = wins;
        this.losses = losses;
        this.totalPlays = totalPlays;
        this.regionId = regionId;
    }
}