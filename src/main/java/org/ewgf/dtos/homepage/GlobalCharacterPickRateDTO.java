package org.ewgf.dtos.homepage;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class GlobalCharacterPickRateDTO {
    private String characterId;
    private Float pickRate;
}
