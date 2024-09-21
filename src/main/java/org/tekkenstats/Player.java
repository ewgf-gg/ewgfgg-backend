package org.tekkenstats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@JsonPropertyOrder({"userId","polarisId", "name","tekkenPower","tekkenPower", "danRank","rating","wins","losses","winRate","playerNames"})
@Document(collection = "player-data")
@Data
public class Player {

    @Id @JsonProperty("userID")
    private String userId; // Corresponds to player1UserID or player2UserID in Battle
    @JsonProperty("name")
    private String name;
    @JsonProperty("polarisID")
    private String polarisId;
    @JsonProperty("tekkenPower")
    private long tekkenPower;
    @JsonProperty("playerNames")
    private List<String> playerNames;
    @JsonProperty("characterStats")
    private Map<String, CharacterStats> characterStats;

    public Player()
    {
        this.userId = "0";
        this.name = "undefined";
        this.polarisId = "0";
        this.tekkenPower = 0;
        this.playerNames = new ArrayList<>();
        this.characterStats = new HashMap<>();
    }

    // Inner class representing character-specific stats
    @Data
    public static class CharacterStats
    {
        @JsonProperty("latestBattle")
        private long latestBattle;
        @JsonProperty("wins")
        private int wins;
        @JsonProperty("losses")
        private int losses;
        @JsonProperty("danRank")
        private int danRank;
        @JsonProperty("rating")
        private int rating;

        @Transient
        private int winsIncrement = 0;
        @Transient
        private int lossIncrement = 0;
        @Transient
        private int ratingChange = 0;

        public CharacterStats()
        {
            this.wins = 0;
            this.losses = 0;
            this.danRank = 0;
            this.rating = 0;
            this.latestBattle = 0;
        }

        public CharacterStats(int wins, int losses, int danRank, int rating,long latestBattle)
        {
            this.wins = wins;
            this.losses = losses;
            this.danRank = danRank;
            this.rating = rating;
            this.latestBattle = latestBattle;
        }

    }
}
