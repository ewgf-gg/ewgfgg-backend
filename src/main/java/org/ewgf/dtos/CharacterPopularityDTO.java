package org.ewgf.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CharacterPopularityDTO
{
   private RegionalCharacterPopularityDTO allRanks;
   private RegionalCharacterPopularityDTO highRank;
   private RegionalCharacterPopularityDTO mediumRank;
   private RegionalCharacterPopularityDTO lowRank;


   public CharacterPopularityDTO(RegionalCharacterPopularityDTO highRank,
                                 RegionalCharacterPopularityDTO mediumRank,
                                 RegionalCharacterPopularityDTO lowRank)
   {
      this.highRank = highRank;
      this.mediumRank = mediumRank;
      this.lowRank = lowRank;
   }
}
