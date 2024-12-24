package org.ewgf.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankWinrateChangesDTO {
    private String characterId;
    private Double change;
    private String trend;
    private String rankCategory;

    // Optional constructor for grouping data
    public RankWinrateChangesDTO(String characterId, double change, String trend) {
        this.characterId = characterId;
        this.change = change;
        this.trend = trend;
    }

    // Static grouping method to help with response creation
    public static Map<String, List<RankWinrateChangesDTO>> groupByRankCategory(List<RankWinrateChangesDTO> changes) {
        return changes.stream()
                .collect(Collectors.groupingBy(
                        RankWinrateChangesDTO::getRankCategory,
                        Collectors.toList()
                ));
    }
}