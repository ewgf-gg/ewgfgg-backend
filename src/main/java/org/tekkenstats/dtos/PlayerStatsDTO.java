package org.tekkenstats.dtos;

import lombok.Data;
import org.tekkenstats.models.Battle;
import org.tekkenstats.models.CharacterStatsId;

import java.util.List;
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
    private List<PlayerBattlesDTO> battles;
}
