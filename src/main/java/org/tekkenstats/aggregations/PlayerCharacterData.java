package org.tekkenstats.aggregations;

import lombok.Data;

@Data
public class PlayerCharacterData {
    private String characterId;
    private int danRank;
    private int wins;
    private int losses;
    private int totalPlays;
    private int gameVersion;
    private Integer regionID;
    private Integer areaID;

    public PlayerCharacterData(String characterId, int danRank, int wins, int losses, int totalPlays, int regionID, int areaID)
    {
        this.characterId = characterId;
        this.danRank = danRank;
        this.wins = wins;
        this.losses = losses;
        this.totalPlays = totalPlays;
        this.regionID = regionID;
        this.areaID = areaID;
    }

    // Constructor, getters, and setters
}