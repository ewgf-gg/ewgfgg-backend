package org.tekkenstats.interfaces;

public interface PlayerBattlesProjection
{
    String getDate();
    String getPlayer1Name();
    Integer getPlayer1CharacterID();
    String getPlayer2Name();
    Integer getPlayer2CharacterID();
    Integer getPlayer1RoundsWon();
    Integer getPlayer2RoundsWon();
    Integer getWinner();
    Integer getStageId();

}
