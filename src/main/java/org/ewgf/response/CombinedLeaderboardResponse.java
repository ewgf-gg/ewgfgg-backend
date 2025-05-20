package org.ewgf.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CombinedLeaderboardResponse {
    @JsonProperty("rankPointsLeaderboard")
    List<LeaderboardEntryResponse> rankPointsLeaderboard;

    @JsonProperty("tekkenProwessLeaderboard")
    List<LeaderboardEntryResponse> tekkenProwessLeaderboard;
}
