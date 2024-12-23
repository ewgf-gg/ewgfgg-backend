package org.tekkenstats.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class CharacterPopularityDTO
{
   private RegionalCharacterPopularityDTO highRank;
   private RegionalCharacterPopularityDTO mediumRank;
   private RegionalCharacterPopularityDTO lowRank;
}
