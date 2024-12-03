package org.tekkenstats.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.tekkenstats.mappers.enumsMapper;

import java.util.*;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
public class Player {

    @Id
    @Column(name = "player_id", unique = true, nullable = false)
    private String playerId;

    @Column(name = "latest_battle")
    private Long latestBattle;

    @Column(name = "name")
    private String name;

    @Column(name = "polaris_id")
    private String polarisId;

    @Column(name = "tekken_power")
    private Long tekkenPower;

    @Column(name = "region_id")
    private Integer regionId;

    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "language")
    private String language;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<PastPlayerNames> playerNames = new HashSet<>();

    // Update the map to use a composite key of character ID and game version
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKeyJoinColumns({
            @MapKeyJoinColumn(name = "character_id"),
            @MapKeyJoinColumn(name = "game_version")
    })
    private Map<CharacterStatsId, CharacterStats> characterStats = new HashMap<>();


    //this constructor is for the dto conversion within the Player Controller class
    public Player(
            String playerId,
            String name,
            String polarisId,
            Long tekkenPower,
            Integer regionId,
            Integer areaId,
            String language,
            Long latestBattle)
    {
        this.playerId = playerId;
        this.name = name;
        this.polarisId = polarisId;
        this.tekkenPower = tekkenPower;
        this.regionId = regionId;
        this.areaId = areaId;
        this.language = language;
        this.latestBattle = latestBattle;
    }


    // Logic for setting/updating tekkenPower based on the latest battle
    public void updateTekkenPower(long newPower, long battleTime)
    {
        if (battleTime >= getLatestBattle())
        {
            this.tekkenPower = newPower;
        }
    }

    private Map<String, String> getMostPlayedCharacterInLatestVersion()
    {
        Map<String, String> result = new HashMap<>();

        if (characterStats == null || characterStats.isEmpty()) {
            result.put("characterId", "No Character Data");
            result.put("danRank", "0");
            return result;
        }

        // Find the latest version
        int latestVersion = characterStats.keySet().stream()
                .mapToInt(CharacterStatsId::getGameVersion)
                .max()
                .orElse(0);

        // If no valid version found
        if (latestVersion == 0) {
            result.put("characterId", "No Character Data");
            result.put("danRank", "0");
            return result;
        }

        // Find the most played character in the latest version
        return characterStats.entrySet().stream()
                .filter(entry -> entry.getKey().getGameVersion() == latestVersion)
                .max(Comparator.comparingInt(entry -> {
                    CharacterStats stats = entry.getValue();
                    return stats.getWins() + stats.getLosses();
                }))
                .map(entry -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("characterId", entry.getKey().getCharacterId());
                    map.put("danRank", String.valueOf(entry.getValue().getDanRank()));
                    return map;
                })
                .orElseGet(() -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("characterId", "No Character Data");
                    map.put("danRank", "0");
                    return map;
                });
    }

    public Map<String, String> getMostPlayedCharacterInfo(enumsMapper mapper)
    {
        Map<String, String> stats = getMostPlayedCharacterInLatestVersion();
        Map<String, String> result = new HashMap<>();

        if (stats.get("characterId").equals("No Character Data"))
        {
            result.put("characterName", "No Character Data");
            result.put("danRank", "N/A");
        }
        else
        {
            result.put("characterName", mapper.getCharacterName(stats.get("characterId")));
            result.put("danRank", mapper.getDanName(stats.get("danRank")));
        }

        return result;
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
