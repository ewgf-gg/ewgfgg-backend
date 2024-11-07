package org.tekkenstats.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class CharacterWinratesDTO {
    private Map<String, Double> highRank;
    private Map<String, Double> mediumRank;
    private Map<String, Double> lowRank;

    public CharacterWinratesDTO(Map<String, Double> highRank, Map<String, Double> mediumRank, Map<String, Double> lowRank)
    {
        this.highRank = highRank;
        this.mediumRank = mediumRank;
        this.lowRank = lowRank;
    }
}
