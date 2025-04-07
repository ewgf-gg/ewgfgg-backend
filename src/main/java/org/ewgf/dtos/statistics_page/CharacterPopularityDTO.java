package org.ewgf.dtos.statistics_page;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CharacterPopularityDTO {
   private RegionalCharacterPopularityDTO allRanks;
   private RegionalCharacterPopularityDTO masterRanks;
   private RegionalCharacterPopularityDTO advancedRanks;
   private RegionalCharacterPopularityDTO intermediateRanks;
   private RegionalCharacterPopularityDTO beginnerRanks;

   //constructer for top 5 global stats
   public CharacterPopularityDTO(RegionalCharacterPopularityDTO masterRanks,
                                 RegionalCharacterPopularityDTO advancedRanks,
                                 RegionalCharacterPopularityDTO intermediateRanks,
                                 RegionalCharacterPopularityDTO beginnerRanks) {
      this.masterRanks = masterRanks;
      this.advancedRanks = advancedRanks;
      this.intermediateRanks = intermediateRanks;
      this.beginnerRanks = beginnerRanks;
   }
}
