package org.tekkenstats;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class CharacterStatsId implements Serializable
{
    @Column(name = "player_id")
    private String playerId;

    @Column(name = "character_id")
    private String characterId;
}
