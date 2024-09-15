package org.tekkenstats.mdbDocuments;

import org.bson.Document;
import org.tekkenstats.Player;
import org.tekkenstats.Battle;

import java.util.List;

public class PlayerDocument extends Document {

    public PlayerDocument() {
        super();
    }

    public PlayerDocument(Player player) {
        super();
        this.put("userId", player.getUserId());
        this.put("name", player.getName());
        this.put("polarisId", player.getPolarisId());
        this.put("tekkenPower", player.getTekkenPower());
        this.put("rating", player.getRating());
        this.put("danRank", player.getDanRank());
        this.put("winRate", player.getWinRate());
        this.put("wins", player.getWins());
        this.put("losses", player.getLosses());
        this.put("playerNames", player.getPlayerNames());
        // For Last10Battles, we'll store the battle IDs instead of the full Battle objects
    }

    public String getUserId() {
        return getString("userId");
    }

    public void setUserId(String userId) {
        put("userId", userId);
    }

    public String getName() {
        return getString("name");
    }

    public void setName(String name) {
        put("name", name);
    }

    public String getPolarisId() {
        return getString("polarisId");
    }

    public void setPolarisId(String polarisId) {
        put("polarisId", polarisId);
    }

    public long getTekkenPower() {
        return getLong("tekkenPower");
    }

    public void setTekkenPower(long tekkenPower) {
        put("tekkenPower", tekkenPower);
    }

    public int getRating() {
        return getInteger("rating");
    }

    public void setRating(int rating) {
        put("rating", rating);
    }

    public int getDanRank() {
        return getInteger("danRank");
    }

    public void setDanRank(int danRank) {
        put("danRank", danRank);
    }

    public double getWinRate() {
        return  getDouble("winRate");
    }

    public void setWinRate(float winRate) {
        put("winRate", (double) winRate);
    }

    public int getWins() {
        return getInteger("wins");
    }

    public void setWins(int wins) {
        put("wins", wins);
    }

    public int getLosses() {
        return getInteger("losses");
    }

    public void setLosses(int losses) {
        put("losses", losses);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPlayerNames() {
        return (List<String>) get("playerNames");
    }

    public void setPlayerNames(List<String> playerNames) {
        put("playerNames", playerNames);
    }

    public void setLast10BattleIds(List<String> last10Battles) {
        put("last10Battles", last10Battles);
    }

    public static PlayerDocument fromPlayer(Player player) {
        return new PlayerDocument(player);
    }
}