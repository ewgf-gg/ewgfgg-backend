package org.ewgf.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RecentlyActivePlayersDTO {
    private String name;
    private Map<String,String> characterAndRank;
    private Long tekkenPower;
    private String polarisId;
    private Integer region;
    private Long lastSeen;
}
