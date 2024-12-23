package org.tekkenstats.interfaces;

public interface WinrateChangesProjection {
    String getCharacterId();
    Double getChange();
    String getTrend();
    String getRankCategory();
}
