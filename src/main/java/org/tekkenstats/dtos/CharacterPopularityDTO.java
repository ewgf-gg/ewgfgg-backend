package org.tekkenstats.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class CharacterPopularityDTO
{
    private Map<String, Long> highRank;
    private Map<String, Long> mediumRank;
    private Map<String, Long> lowRank;

    public CharacterPopularityDTO(Map<String, Long> highRank, Map<String, Long> mediumRank, Map<String, Long> lowRank)
    {
        this.highRank = highRank;
        this.mediumRank = mediumRank;
        this.lowRank = lowRank;
    }
}
