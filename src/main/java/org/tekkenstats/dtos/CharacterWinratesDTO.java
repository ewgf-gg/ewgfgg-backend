package org.tekkenstats.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class CharacterWinratesDTO {
    private RegionalCharacterWinrateDTO highRank;
    private RegionalCharacterWinrateDTO mediumRank;
    private RegionalCharacterWinrateDTO lowRank;

    public CharacterWinratesDTO(
            RegionalCharacterWinrateDTO highRank,
            RegionalCharacterWinrateDTO mediumRank,
            RegionalCharacterWinrateDTO lowRank) {
        this.highRank = highRank;
        this.mediumRank = mediumRank;
        this.lowRank = lowRank;
    }
}
