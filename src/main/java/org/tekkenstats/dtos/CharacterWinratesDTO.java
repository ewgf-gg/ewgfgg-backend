package org.tekkenstats.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class CharacterWinratesDTO {
    private Map<String, Double> highRank;
    private Map<String, Double> lowRank;

    public CharacterWinratesDTO(Map<String, Double> highRank, Map<String, Double> lowRank) {
        this.highRank = highRank;
        this.lowRank = lowRank;
    }
}
