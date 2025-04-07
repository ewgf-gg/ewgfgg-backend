package org.ewgf.dtos.statistics_page;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RegionalCharacterWinrateDTO {
    private Map<String, Double> globalStats;
    private Map<String, Map<String, Double>> regionalStats;
}
