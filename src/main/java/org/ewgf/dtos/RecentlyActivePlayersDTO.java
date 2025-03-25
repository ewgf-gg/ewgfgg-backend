package org.ewgf.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentlyActivePlayersDTO {
    private String name;
    private String character;
    private Long tekkenPower;
    private Integer region;
    private LocalDateTime lastSeen;
}
