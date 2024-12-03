package org.tekkenstats.dtos;

import lombok.Data;

@Data
public class PlayerBattleDTO
{
    private String date;
    private String player1Name;
    private Integer player1CharacterId;
    private Integer player1RegionId;
    private Integer player1DanRank;
    private String player2Name;
    private Integer player2CharacterId;
    private Integer player2RegionId;
    private Integer player2DanRank;
    private Integer player1RoundsWon;
    private Integer player2RoundsWon;
    private Integer winner;
    private Integer StageId;

}
