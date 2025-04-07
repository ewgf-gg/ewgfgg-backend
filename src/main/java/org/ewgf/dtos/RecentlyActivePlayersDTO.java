package org.ewgf.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class RecentlyActivePlayersDTO {
    private String name;
    private Map<String,String> characterAndRank;
    private Long tekkenPower;
    private String polarisId;
    private Integer region;
    private Long lastSeen;
}
