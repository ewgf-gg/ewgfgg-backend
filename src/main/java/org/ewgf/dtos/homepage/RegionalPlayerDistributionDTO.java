package org.ewgf.dtos.homepage;

import lombok.Data;

@Data
public class RegionalPlayerDistributionDTO {
    private Float asia;
    private Float europe;
    private Float americas;
    private Float oceania;
    private Float middleEast;
    private Float unassigned;
}
