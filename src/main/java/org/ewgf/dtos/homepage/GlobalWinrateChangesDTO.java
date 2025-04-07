package org.ewgf.dtos.homepage;


import lombok.Data;

@Data
public class GlobalWinrateChangesDTO {
    private String characterId;
    private Double change;
    private String trend;
}
