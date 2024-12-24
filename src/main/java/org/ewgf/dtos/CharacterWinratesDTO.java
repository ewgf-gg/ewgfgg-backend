package org.ewgf.dtos;

import lombok.Data;

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
