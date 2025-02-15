package org.ewgf.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@NoArgsConstructor
@Data
public class PlayerMetadataDTO {
    String playerName;
    String polarisId;
    Integer regionId;
    Integer areaId;
    String latestBattleDate;
    Long tekkenPower;
    Map<String,String> mainCharacterAndRank;
}
