package org.ewgf.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaderboardEntryResponse {
    @JsonProperty("ranking")
    private Integer ranking;

    @JsonProperty("playerName")
    private String name;

    @JsonProperty("polarisId")
    private String polarisId;

    @JsonProperty("platform")
    private Integer platform;

    @JsonProperty("score")
    private Integer score;

    @JsonProperty("rank")
    private Integer danRank;

    @JsonProperty("charaId")
    private String characterId;
}
