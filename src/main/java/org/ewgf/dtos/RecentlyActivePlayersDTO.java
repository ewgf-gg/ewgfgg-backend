package org.ewgf.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RecentlyActivePlayersDTO {
    private String name;
    private Map<String,String> characterAndRank;
    private Long tekkenPower;
    private Integer region;
    private LocalDateTime lastSeen;
}
