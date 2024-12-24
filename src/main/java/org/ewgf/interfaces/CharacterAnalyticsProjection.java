package org.ewgf.interfaces;

public interface CharacterAnalyticsProjection {
    String getCharacterId();
    long getTotalWins();
    long getTotalLosses();
    double getWinratePercentage();
    long getTotalBattles();
    Integer getGameVersion();
    String getRankCategory();
    String getRegionId();
}
