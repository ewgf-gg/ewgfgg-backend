package org.ewgf.dtos;

import lombok.Data;

import lombok.NoArgsConstructor;
import org.ewgf.models.CharacterStatsId;

import java.util.*;

@Data
@NoArgsConstructor
public class PlayerDTO {
    private String polarisId;
    private String name;
    private Integer regionId;
    private Integer areaId;
    private long tekkenPower;
    private long latestBattle;
    private Map<String, String> mainCharacterAndRank = new HashMap<>();
    private Map<String, PlayerMatchupSummaryDTO> playedCharacters = new HashMap<>();
    private List<BattleDTO> battles = new ArrayList<>();
}
