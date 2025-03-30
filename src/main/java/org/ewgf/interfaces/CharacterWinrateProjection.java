package org.ewgf.interfaces;

public interface CharacterWinrateProjection {
    String getCharacterId();
    Long getTotalWins();
    Long getTotalLosses();
    Integer getGameVersion();
    String getRankCategory();
    String getRegionId();
}