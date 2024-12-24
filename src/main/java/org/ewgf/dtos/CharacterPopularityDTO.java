package org.ewgf.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CharacterPopularityDTO
{
   private RegionalCharacterPopularityDTO highRank;
   private RegionalCharacterPopularityDTO mediumRank;
   private RegionalCharacterPopularityDTO lowRank;
}
