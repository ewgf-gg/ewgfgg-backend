package org.tekkenstats.interfaces;

public interface PlayerAnalyticsProjection {
    String getPlayerId();
    String getCharacterId();
    Integer getDanRank();
    Integer getWins();
    Integer getLosses();
    Integer getRegionId();
    Integer getAreaId();

    default Integer getTotalPlays() {
        return (getWins() != null && getLosses() != null) ? getWins() + getLosses() : 0;
    }
}
