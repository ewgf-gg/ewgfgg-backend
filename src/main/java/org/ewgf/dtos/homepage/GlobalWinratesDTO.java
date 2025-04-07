package org.ewgf.dtos.homepage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GlobalWinratesDTO {
    private String characterId;
    private Float winRate;
}
