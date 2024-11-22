package org.tekkenstats.dtos;

import lombok.Data;

@Data
public class PlayerBattlesDTO
{
    private String date;
    private String player1Name;
    private Integer player1CharacterID;
    private String player2Name;
    private Integer player2CharacterID;
    private Integer player1RoundsWon;
    private Integer player2RoundsWon;
    private Integer winner;
    private Integer StageId;

}
