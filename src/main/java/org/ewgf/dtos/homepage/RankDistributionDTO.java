package org.ewgf.dtos.homepage;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RankDistributionDTO
{
    private List<RankDistributionEntry> overall = new ArrayList<>();
    private List<RankDistributionEntry> standard = new ArrayList<>();

    public void addDistribution(String category, RankDistributionEntry entry) {
        if ("overall".equals(category)) {
            overall.add(entry);
        } else if ("standard".equals(category)) {
            standard.add(entry);
        }
    }
}
