package org.ewgf.interfaces;

public interface CharacterAnalyticsProjection {
    String getCharacterId();
    double getWinratePercentage();
    long getTotalBattles();
    Integer getGameVersion();
    String getRankCategory();
    String getRegionId();
}
