package org.tekkenstats.dtos;

import lombok.Data;
import org.tekkenstats.models.CharacterStatsId;

@Data
public class CharacterStatsDTO
{
    private String characterName;
    private String danName;
    private int danRank;
    private int wins;
    private int losses;
}
