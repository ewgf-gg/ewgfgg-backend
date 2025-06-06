package org.ewgf.dtos;

import lombok.Data;

@Data
public class PlayerSearchDTO
{
    private String id;
    private String name;
    private String tekkenId;
    private String formattedTekkenId;
    private Integer regionId;
    private String mostPlayedCharacter;
    private String DanRankName;
}
