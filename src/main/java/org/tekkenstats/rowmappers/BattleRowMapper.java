package org.tekkenstats.rowmappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tekkenstats.models.Battle;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BattleRowMapper implements RowMapper<Battle> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Battle mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        Battle battle = new Battle();
        battle.setBattleId(rs.getString("battle_id"));
        battle.setDate(rs.getString("date"));
        battle.setBattleAt(rs.getLong("battle_at"));
        battle.setBattleType(rs.getInt("battle_type"));
        battle.setGameVersion(rs.getInt("game_version"));
        battle.setPlayer1CharacterID(rs.getInt("player1_character_id"));
        battle.setPlayer1Name(rs.getString("player1_name"));
        battle.setPlayer1PolarisID(rs.getString("player1_polaris_id"));
        battle.setPlayer1TekkenPower(rs.getInt("player1_tekken_power"));
        battle.setPlayer1DanRank(rs.getInt("player1_dan_rank"));
        battle.setPlayer1RatingBefore(rs.getInt("player1_rating_before"));
        battle.setPlayer1RatingChange(rs.getInt("player1_rating_change"));
        battle.setPlayer1RoundsWon(rs.getInt("player1_rounds_won"));
        battle.setPlayer1UserID(rs.getString("player1_id"));
        battle.setPlayer2CharacterID(rs.getInt("player2_character_id"));
        battle.setPlayer2Name(rs.getString("player2_name"));
        battle.setPlayer2PolarisID(rs.getString("player2_polaris_id"));
        battle.setPlayer2TekkenPower(rs.getInt("player2_tekken_power"));
        battle.setPlayer2DanRank(rs.getInt("player2_dan_rank"));
        battle.setPlayer2RatingBefore(rs.getInt("player2_rating_before"));
        battle.setPlayer2RatingChange(rs.getInt("player2_rating_change"));
        battle.setPlayer2RoundsWon(rs.getInt("player2_rounds_won"));
        battle.setPlayer2UserID(rs.getString("player2_id"));
        battle.setStageID(rs.getInt("stageid"));
        battle.setWinner(rs.getInt("winner"));
        return battle;
    }

    //values names from json api are different from postgres table names
    public List<Battle> mapToBattlesFromApiJson(String jsonString) throws JsonProcessingException {
        JsonNode jsonArray = objectMapper.readTree(jsonString);
        List<Battle> battles = new ArrayList<>();

        for (JsonNode node : jsonArray)
        {
            Battle battle = new Battle();
            battle.setBattleAt(node.get("battle_at").asLong());
            battle.setBattleId(node.get("battle_id").asText());
            battle.setBattleType(node.get("battle_type").asInt());
            battle.setGameVersion(node.get("game_version").asInt());
            battle.setPlayer1CharacterID(node.get("p1_chara_id").asInt());
            battle.setPlayer1Name(node.get("p1_name").asText());
            battle.setPlayer1PolarisID(node.get("p1_polaris_id").asText());
            battle.setPlayer1TekkenPower(node.get("p1_power").asInt());
            battle.setPlayer1DanRank(node.get("p1_rank").asInt());
            battle.setPlayer1RatingBefore(node.get("p1_rating_before").asInt());
            battle.setPlayer1RatingChange(node.get("p1_rating_change").asInt());
            battle.setPlayer1RoundsWon(node.get("p1_rounds").asInt());
            battle.setPlayer1UserID(node.get("p1_user_id").asText());
            battle.setPlayer2CharacterID(node.get("p2_chara_id").asInt());
            battle.setPlayer2Name(node.get("p2_name").asText());
            battle.setPlayer2PolarisID(node.get("p2_polaris_id").asText());
            battle.setPlayer2TekkenPower(node.get("p2_power").asInt());
            battle.setPlayer2DanRank(node.get("p2_rank").asInt());
            battle.setPlayer2RatingBefore(node.get("p2_rating_before").asInt());
            battle.setPlayer2RatingChange(node.get("p2_rating_change").asInt());
            battle.setPlayer2RoundsWon(node.get("p2_rounds").asInt());
            battle.setPlayer2UserID(node.get("p2_user_id").asText());
            battle.setStageID(node.get("stage_id").asInt());
            battle.setWinner(node.get("winner").asInt());
            battles.add(battle);
        }
        return battles;
    }
}

