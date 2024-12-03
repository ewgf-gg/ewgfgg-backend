package org.tekkenstats.interfaces;

public interface PlayerWithBattlesProjection
{
    // Player data
    String getPlayerId();
    String getName();
    String getPolarisId();
    Long getTekkenPower();
    Integer getRegionId();
    Integer getAreaId();
    String getLanguage();
    Long getLatestBattle();

    // Character stats data
    String getCharacterId();
    Integer getGameVersion();
    Integer getDanRank();
    Integer getWins();
    Integer getLosses();
    Long getCharacterLatestBattle();

    // Battle data
    String getDate();
    String getPlayer1Name();
    Integer getPlayer1CharacterId();
    Integer getPlayer1RegionId();
    Integer getPlayer1DanRank();
    String getPlayer2Name();
    Integer getPlayer2CharacterId();
    Integer getPlayer2RegionId();
    Integer getPlayer2DanRank();
    Integer getPlayer1RoundsWon();
    Integer getPlayer2RoundsWon();
    Integer getWinner();
    Integer getStageId();
}
