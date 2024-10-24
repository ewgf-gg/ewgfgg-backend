package org.tekkenstats.aggregations;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;


@Embeddable
public class AggregatedStatisticId implements Serializable {

    @Column(name = "game_version")
    private int gameVersion;

    @Column(name = "character_id")
    private String characterId;

    @Column(name = "dan_rank")
    private int danRank;

    @Column(name = "category")
    private String category; // 'standard' or 'overall'

    // Constructors
    public AggregatedStatisticId() {
    }

    public AggregatedStatisticId(int gameVersion, String characterId, int danRank, String category) {
        this.gameVersion = gameVersion;
        this.characterId = characterId;
        this.danRank = danRank;
        this.category = category;
    }

    // Getters and setters
    // ...

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregatedStatisticId that = (AggregatedStatisticId) o;

        return gameVersion == that.gameVersion &&
                danRank == that.danRank &&
                Objects.equals(characterId, that.characterId) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameVersion, characterId, danRank, category);
    }
}
