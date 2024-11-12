package org.tekkenstats.dtos;

import lombok.Data;
import org.tekkenstats.models.CharacterStatsId;

import java.util.Map;

@Data
public class PlayerStatsDTO
{
    private String playerId;
    private String name;
    private Integer region;
    private long tekkenPower;
    private long latestBattle;
    private Map<CharacterStatsId, CharacterStatsDTO> characterStats;
}
