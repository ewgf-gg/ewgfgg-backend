package org.tekkenstats.interfaces;

public interface BattlesProjection
{
    String getDate();
    Integer getGameVersion();
    String getPlayer1Name();
    String getPlayer1PolarisId();
    Integer getPlayer1CharacterId();
    Integer getPlayer1RegionId();
    Long getPlayer1TekkenPower();
    Integer getPlayer1DanRank();
    String getPlayer2Name();
    String getPlayer2PolarisId();
    Integer getPlayer2CharacterId();
    Integer getPlayer2RegionId();
    Integer getPlayer2DanRank();
    Long getPlayer2TekkenPower();
    Integer getPlayer1RoundsWon();
    Integer getPlayer2RoundsWon();
    Integer getWinner();
    Integer getStageId();
}
