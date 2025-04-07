package org.ewgf.dtos.homepage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegionalPlayerDistributionDTO {
    private Float asia;
    private Float europe;
    private Float americas;
    private Float oceania;
    private Float middleEast;
    private Float unassigned;
}
