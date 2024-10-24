package org.tekkenstats.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class rankDistributionDTO
{
    private Map<Integer,Double> rankDistribution;
}
