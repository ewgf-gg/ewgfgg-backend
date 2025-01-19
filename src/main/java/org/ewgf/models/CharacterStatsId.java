package org.ewgf.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterStatsId implements Serializable {

    @Column(name = "player_id", insertable=false, updatable=false)
    private String playerId;

    @Column(name = "character_id")
    private String characterId;

    @Column(name = "game_version")
    private int gameVersion;

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
