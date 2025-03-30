package org.ewgf.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CharacterWinratesDTO {
    private RegionalCharacterWinrateDTO allRanks;
    private RegionalCharacterWinrateDTO masterRanks;
    private RegionalCharacterWinrateDTO advancedRanks;
    private RegionalCharacterWinrateDTO intermediateRanks;
    private RegionalCharacterWinrateDTO beginnerRanks;

    //constructor for top 5 call
    public CharacterWinratesDTO(RegionalCharacterWinrateDTO masterRanks,
                                RegionalCharacterWinrateDTO advancedRanks,
                                RegionalCharacterWinrateDTO intermediateRanks,
                                RegionalCharacterWinrateDTO beginnerRanks) {
        this.masterRanks = masterRanks;
        this.advancedRanks = advancedRanks;
        this.intermediateRanks = intermediateRanks;
        this.beginnerRanks = beginnerRanks;
    }
}
