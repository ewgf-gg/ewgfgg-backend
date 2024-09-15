package org.tekkenstats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({"userId","polarisId", "name","tekkenPower","tekkenPower", "danRank","rating","wins","losses","winRate","playerNames"})
@Document(collection = "player-data")
@Data
public class Player {

    @Id
    private String userId; // Corresponds to player1UserID or player2UserID in Battle
    private String name;
    private String polarisId;
    private long tekkenPower;
    private int rating; //glicko2 rating
    private int danRank;
    private double winRate;
    private int wins;
    private int losses;
    private long latestBattle;

    @Transient
    private int winsIncrement = 0;
    @Transient
    private int lossIncrement = 0;
    @Transient
    private int ratingChange = 0;

    private List<String> playerNames;



}
