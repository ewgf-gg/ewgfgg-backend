package org.ewgf.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@NoArgsConstructor
@Data
public class PlayerMetadataDTO {
    private String playerName;
    private String polarisId;
    private Integer regionId;
    private String latestBattleDate;
    private Long tekkenPower;
    private Map<String,String> mainCharacterAndRank;
}
