package org.ewgf.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
public class CharacterStatsId implements Serializable {

    @Column(name = "player_id", insertable=false, updatable=false)
    private String playerId;

    @Column(name = "character_id")
    private String characterId;

    @Column(name = "game_version")
    private int gameVersion;

    public CharacterStatsId(String playerId, String characterId, Integer gameVersion)
    {
        this.playerId = playerId;
        this.characterId = characterId;
        this.gameVersion = gameVersion;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterStatsId that = (CharacterStatsId) o;
        return Objects.equals(playerId, that.playerId) &&
                Objects.equals(characterId, that.characterId) &&
                Objects.equals(gameVersion, that.gameVersion);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(playerId, characterId, gameVersion);
    }
}
