package org.tekkenstats.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class CharacterPopularityDTO
{
    Map<String,Integer> characterPopularity;
}
