package org.tekkenstats.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PlayerBattleDTO
{
    private String date;
    private Integer gameVersion;
    private String player1Name;
    private String player1PolarisId;
    private Integer player1CharacterId;
    private Integer player1RegionId;
    private Integer player1DanRank;
    private Long player1TekkenPower;
    private String player2Name;
    private String player2PolarisId;
    private Integer player2CharacterId;
    private Integer player2RegionId;
    private Integer player2DanRank;
    private Long player2TekkenPower;
    private Integer player1RoundsWon;
    private Integer player2RoundsWon;
    private Integer winner;
    private Integer StageId;

    public PlayerBattleDTO(String date,
                           Integer gameVersion,
                           String player1Name,
                           String player1PolarisId,
                           Integer player1CharacterId,
                           Integer player1RegionId,
                           Long player1TekkenPower,
                           Integer player1DanRank,
                           String player2Name,
                           String player2PolarisId,
                           Integer player2RegionId,
                           Integer player2CharacterId,
                           Integer player2DanRank,
                           Long player2TekkenPower,
                           Integer player1RoundsWon,
                           Integer player2RoundsWon,
                           Integer winner,
                           Integer stageId)
    {
        this.date = date;
        this.gameVersion = gameVersion;
        this.player1Name = player1Name;
        this.player1PolarisId = player1PolarisId;
        this.player1CharacterId = player1CharacterId;
        this.player1RegionId = player1RegionId;
        this.player1DanRank = player1DanRank;
        this.player1TekkenPower = player1TekkenPower;
        this.player2Name = player2Name;
        this.player2PolarisId = player2PolarisId;
        this.player2CharacterId = player2CharacterId;
        this.player2RegionId = player2RegionId;
        this.player2DanRank = player2DanRank;
        this.player2TekkenPower = player2TekkenPower;
        this.player1RoundsWon = player1RoundsWon;
        this.player2RoundsWon = player2RoundsWon;
        this.winner = winner;
        this.StageId = stageId;

    }
}
