package org.tekkenstats;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@Document(collection = "replay-data")
@Data
public class Battle {

    @Id @JsonProperty("battle_id")
    private String battleId;
    @JsonProperty
    private String Date;
    @JsonProperty("battle_at")
    private long battleAt;
    @JsonProperty("battle_type")
    private int battleType;
    @JsonProperty("game_version")
    private int gameVersion;

    @JsonProperty("p1_chara_id")
    private int player1CharacterID; // ID for playable character
    @JsonProperty("p1_name")
    private String player1Name;
    @JsonProperty("p1_polaris_id")
    private String player1PolarisID;
    @JsonProperty("p1_power")
    private long player1TekkenPower;
    @JsonProperty("p1_rank")
    private int player1DanRank;
    @JsonProperty("p1_rating_before")
    private Integer player1RatingBefore; // it is possible for this field to be null, so wrapper class is used instead
    @JsonProperty("p1_rating_change")
    private Integer player1RatingChange; //it is possible for this field to be null, so wrapper class is used instead
    @JsonProperty("p1_rounds")
    private int player1RoundsWon;
    @JsonProperty("p1_user_id")
    private String player1UserID; // this is a huge number, so I just cast it to a string instead


    @JsonProperty("p2_chara_id")
    private int player2CharacterID;
    @JsonProperty("p2_name")
    private String player2Name;
    @JsonProperty("p2_polaris_id")
    private String player2PolarisID;
    @JsonProperty("p2_power")
    private long player2TekkenPower;
    @JsonProperty("p2_rank")
    private int player2DanRank;
    @JsonProperty("p2_rating_before")
    private Integer player2RatingBefore;
    @JsonProperty("p2_rating_change")
    private Integer player2RatingChange;
    @JsonProperty("p2_rounds")
    private int player2RoundsWon;
    @JsonProperty("p2_user_id")
    private String player2UserID;

    @JsonProperty("stage_id")
    private int stageID;
    @JsonProperty("winner")
    private int winner; // 1 or 2 respectively


    // Getters and Setters generated automatically thanks to lombok
}
