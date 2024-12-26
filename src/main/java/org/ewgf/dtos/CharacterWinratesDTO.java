package org.ewgf.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CharacterWinratesDTO {
    private RegionalCharacterWinrateDTO allRanks;
    private RegionalCharacterWinrateDTO highRank;
    private RegionalCharacterWinrateDTO mediumRank;
    private RegionalCharacterWinrateDTO lowRank;

    public CharacterWinratesDTO(RegionalCharacterWinrateDTO highRank,
                                RegionalCharacterWinrateDTO mediumRank,
                                RegionalCharacterWinrateDTO lowRank)
    {
        this.highRank = highRank;
        this.mediumRank = mediumRank;
        this.lowRank = lowRank;
    }

}
