package org.tekkenstats;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@Entity
@Table(name = "players")
@Data
public class Player {

    @Id
    @Column(name = "user_id", unique = true, nullable = false)
    private String playerId;

    @Column(name = "latest_battle")
    private long latestBattle;

    @Column(name = "name")
    private String name;

    @Column(name = "polaris_id")
    private String polarisId;

    @Column(name = "tekken_power")
    private long tekkenPower;



    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<PastPlayerNames> playerNames = new HashSet<>();

    // A map to store character stats for each character by character ID
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "id.characterId")
    private Map<String, CharacterStats> characterStats = new HashMap<>();

    public Player() {
        this.playerId = "0";
        this.name = "undefined";
        this.polarisId = "0";
        this.tekkenPower = 0;
    }

    // Logic for setting/updating tekkenPower based on the latest battle
    public void updateTekkenPower(long newPower, long battleTime) {
        if (battleTime >= getLatestBattle())
        {
            this.tekkenPower = newPower;
        }
    }

    public void setLatestBattle()
    {
        this.latestBattle = characterStats.values().stream()
                .mapToLong(CharacterStats::getLatestBattle)
                .max().orElse(0);
    }

    public boolean hasPlayerName(String name) {
        return playerNames.stream().anyMatch(pn -> pn.getName().equals(name));
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return Objects.equals(playerId, player.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

}
