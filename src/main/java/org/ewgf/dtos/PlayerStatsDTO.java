package org.ewgf.dtos;

import lombok.Data;

import lombok.NoArgsConstructor;
import org.ewgf.models.CharacterStatsId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class PlayerStatsDTO
{
    private String playerId;
    private String name;
    private Integer regionId;
    private Integer areaId;
    private long tekkenPower;
    private long latestBattle;
    private Map<String, String> mainCharacterAndRank;
    private Map<CharacterStatsId, CharacterStatsDTO> characterStats;
    private List<PlayerBattleDTO> battles;

    public PlayerStatsDTO(String playerId,
                          String name,
                          Integer regionId,
                          Integer areaId,
                          Long tekkenPower,
                          Long latestBattle)
    {
        this.playerId = playerId;
        this.name = name;
        this.regionId = regionId;
        this.areaId = areaId;
        this.tekkenPower = tekkenPower;
        this.latestBattle = latestBattle;
        characterStats = new HashMap<>();
        battles = new ArrayList<>();

    }
}
