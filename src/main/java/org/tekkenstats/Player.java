package org.tekkenstats;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

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
    private float winRate;
    private int wins;
    private int losses;

    private List<String> playerNames;
    private List<Battle> Last10Battles;


}
