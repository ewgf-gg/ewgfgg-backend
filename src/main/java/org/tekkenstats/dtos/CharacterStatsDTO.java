package org.tekkenstats.dtos;

import lombok.Data;

@Data
public class CharacterStatsDTO
{
    private String characterName;
    private String danName;
    private int danRank;
    private int wins;
    private int losses;

    public CharacterStatsDTO(String characterName, String danName, Integer danRank, Integer wins, Integer losses)
    {
        this.characterName = characterName;
        this.danName = danName;
        this.danRank = danRank;
        this.wins = wins;
        this.losses = losses;
    }
}
