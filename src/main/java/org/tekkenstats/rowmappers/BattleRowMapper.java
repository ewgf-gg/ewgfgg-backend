package org.tekkenstats.rowmappers;

import org.tekkenstats.models.Battle;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BattleRowMapper implements RowMapper<Battle> {

    @Override
    public Battle mapRow(ResultSet rs, int rowNum) throws SQLException {
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
        battle.setPlayer1UserID(rs.getString("player1_userid"));
        battle.setPlayer2CharacterID(rs.getInt("player2_character_id"));
        battle.setPlayer2Name(rs.getString("player2_name"));
        battle.setPlayer2PolarisID(rs.getString("player2_polaris_id"));
        battle.setPlayer2TekkenPower(rs.getInt("player2_tekken_power"));
        battle.setPlayer2DanRank(rs.getInt("player2_dan_rank"));
        battle.setPlayer2RatingBefore(rs.getInt("player2_rating_before"));
        battle.setPlayer2RatingChange(rs.getInt("player2_rating_change"));
        battle.setPlayer2RoundsWon(rs.getInt("player2_rounds_won"));
        battle.setPlayer2UserID(rs.getString("player2_userid"));
        battle.setStageID(rs.getInt("stageid"));
        battle.setWinner(rs.getInt("winner"));
        // Set other properties as needed
        return battle;
    }
}
