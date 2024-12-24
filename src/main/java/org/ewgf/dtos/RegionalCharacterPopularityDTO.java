package org.ewgf.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RegionalCharacterPopularityDTO {
   private Map<String, Long> globalStats;
   private Map<String, Map<String, Long>> regionalStats;
}
