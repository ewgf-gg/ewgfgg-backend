package org.ewgf.dtos.homepage;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GlobalWinrateTrendsDTO {
    private String characterId;
    private Double change;
    private String trend;
}
