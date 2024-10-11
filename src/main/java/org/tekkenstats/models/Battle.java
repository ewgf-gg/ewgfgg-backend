package org.tekkenstats.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "battles")
@Data
public class Battle {

    @Id
    @JsonProperty("battle_id")
    @Column(name = "battle_id", nullable = false)
    private String battleId;

    @JsonProperty("date")
    @Column(name = "date")
    private String date;

    @JsonProperty("battle_at")
    @Column(name = "battle_at", nullable = false)
    private long battleAt;

    @JsonProperty("battle_type")
    @Column(name = "battle_type")
    private int battleType;

    @JsonProperty("game_version")
    @Column(name = "game_version", nullable = false)
    private int gameVersion;

    @JsonProperty("p1_chara_id")
    @Column(name = "player1_character_id", nullable = false)
    private int player1CharacterID;

    @JsonProperty("p1_name")
    @Column(name = "player1_name")
    private String player1Name;

    @JsonProperty("p1_polaris_id")
    @Column(name = "player1_polaris_id")
    private String player1PolarisID;

    @JsonProperty("p1_power")
    @Column(name = "player1_tekken_power", nullable = false)
    private long player1TekkenPower;

    @JsonProperty("p1_rank")
    @Column(name = "player1_dan_rank", nullable = false)
    private int player1DanRank;

    @JsonProperty("p1_rating_before")
    @Column(name = "player1_rating_before")
    private Integer player1RatingBefore;

    @JsonProperty("p1_rating_change")
    @Column(name = "player1_rating_change")
    private Integer player1RatingChange;

    @JsonProperty("p1_rounds")
    @Column(name = "player1_rounds_won", nullable = false)
    private int player1RoundsWon;

    @JsonProperty("p1_user_id")
    @Column(name = "player1_id")
    private String player1UserID;

    @JsonProperty("p2_chara_id")
    @Column(name = "player2_character_id", nullable = false)
    private int player2CharacterID;

    @JsonProperty("p2_name")
    @Column(name = "player2_name")
    private String player2Name;

    @JsonProperty("p2_polaris_id")
    @Column(name = "player2_polaris_id")
    private String player2PolarisID;

    @JsonProperty("p2_power")
    @Column(name = "player2_tekken_power", nullable = false)
    private long player2TekkenPower;

    @JsonProperty("p2_rank")
    @Column(name = "player2_dan_rank", nullable = false)
    private int player2DanRank;

    @JsonProperty("p2_rating_before")
    @Column(name = "player2_rating_before")
    private Integer player2RatingBefore;

    @JsonProperty("p2_rating_change")
    @Column(name = "player2_rating_change")
    private Integer player2RatingChange;

    @JsonProperty("p2_rounds")
    @Column(name = "player2_rounds_won", nullable = false)
    private int player2RoundsWon;

    @JsonProperty("p2_user_id")
    @Column(name = "player2_id")
    private String player2UserID;

    @JsonProperty("stage_id")
    @Column(name = "stageid", nullable = false)
    private int stageID;

    @JsonProperty("winner")
    @Column(name = "winner")
    private int winner;

    // Getters and Setters generated automatically thanks to lombok
}
