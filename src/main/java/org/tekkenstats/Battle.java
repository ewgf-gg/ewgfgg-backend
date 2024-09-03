package org.tekkenstats;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "replay-data")
@Data
public class Battle {

    @Id
    private String battleId;
    private long battleAt;
    private int battleType;
    private int gameVersion;

    private int player1CharacterID; //ID for playable character
    private String player1Name;
    private String player1PolarisID;
    private long player1TekkenPower;
    private int player1DanRank;
    private int player1RatingBefore;
    private int player1RatingChange;
    private int player1RoundsWon;
    private String player1UserID; //this is a huge number, so i just cast it to a string instead

    private int player2CharacterID;
    private String player2Name;
    private String player2PolarisID;
    private long player2TekkenPower;
    private int player2DanRank;
    private int player2RatingBefore;
    private int player2RatingChange;
    private int player2RoundsWon;
    private String player2UserID;

    private int stageID;
    private int winner; // 1 or 2 respectively


    // Getters and Setters generated automatically thanks to lombok
}
