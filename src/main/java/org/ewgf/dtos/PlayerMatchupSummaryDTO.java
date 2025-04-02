package org.ewgf.dtos;

import com.sun.jdi.IntegerValue;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class PlayerMatchupSummaryDTO {
    private Integer wins= 0;
    private Integer losses = 0;
    private Integer currentSeasonDanRank;
    private Integer previousSeasonDanRank;
    private Float characterWinrate;
    private Map<String, Float> bestMatchup = new HashMap<>();
    private Map<String, Float> worstMatchup = new HashMap<>();
    private Map<String, MatchupStat> matchups = new HashMap<>();
}
