package org.tekkenstats.interfaces;

public interface CharacterWinrateProjection {
    String getCharacterId();
    Long getTotalWins();
    Long getTotalLosses();
    Double getWinratePercentage();
}