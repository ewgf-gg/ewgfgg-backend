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

    public PlayerCharacterData(String characterId, int danRank, int wins, int losses, int totalPlays)
    {
        this.characterId = characterId;
        this.danRank = danRank;
        this.wins = wins;
        this.losses = losses;
        this.totalPlays = totalPlays;
    }

    // Constructor, getters, and setters
}