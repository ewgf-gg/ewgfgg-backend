package org.ewgf.dtos;

import lombok.Data;

@Data
public class TekkenStatsSummaryDTO {
   private long totalRankedReplays;
   private long totalPlayers;
   private int totalUnrankedReplays;
}
