package org.ewgf.dtos;

import lombok.Data;

@Data
public class RankDistributionEntry
{
    private Integer rank;
    private Double percentage;

    public RankDistributionEntry(Integer rank, Double percentage)
    {
        this.rank = rank;
        this.percentage = percentage;
    }
}
