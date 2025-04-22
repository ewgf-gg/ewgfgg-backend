package org.ewgf.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.ewgf.utils.TekkenDataMapperUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.ewgf.utils.Constants.*;

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

    private Map<String, String> findMainCharacter() {
        Map<String, String> result = new HashMap<>();

        // Handle empty or null character stats
        if (characterStats == null || characterStats.isEmpty()) {
            result.put(CHARACTER_ID, "No Character Data");
            result.put(DAN_RANK, "0");
            return result;
        }

        // Step 1: Find the highest dan rank achieved by any character
        int highestDanRank = characterStats.values().stream()
                .mapToInt(CharacterStats::getDanRank)
                .max()
                .orElse(0);

        // Step 2: Group characters and sum their total matches
        Map<String, Integer> totalMatchesByCharacter = characterStats.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getCharacterId(),
                        Collectors.summingInt(entry ->
                                entry.getValue().getWins() + entry.getValue().getLosses()
                        )
                ));

        // Step 3: Find characters that reached the highest dan rank
        Set<String> charactersWithHighestRank = characterStats.entrySet().stream()
                .filter(entry -> entry.getValue().getDanRank() == highestDanRank)
                .map(entry -> entry.getKey().getCharacterId())
                .collect(Collectors.toSet());

        // Step 4: Among characters with highest rank, find the one with most total matches
        return charactersWithHighestRank.stream()
                .max(Comparator.comparingInt(totalMatchesByCharacter::get))
                .map(characterId -> {
                    Map<String, String> map = new HashMap<>();
                    map.put(CHARACTER_ID, characterId);
                    map.put(DAN_RANK, String.valueOf(highestDanRank));
                    return map;
                })
                .orElseGet(() -> {
                    Map<String, String> map = new HashMap<>();
                    map.put(CHARACTER_ID, "No Character Data");
                    map.put(DAN_RANK, "0");
                    return map;
                });
    }

    public Map<String, String> getMostPlayedCharacterInfo() {
        Map<String, String> stats = findMainCharacter();
        Map<String, String> result = new HashMap<>();

        if (stats.get(CHARACTER_ID).equals("No Character Data")) {
            result.put(CHARACTER_NAME, "No Character Data");
            result.put(DAN_RANK, "N/A");
        }
        else {
            result.put(CHARACTER_NAME, TekkenDataMapperUtils.getCharacterName(stats.get(CHARACTER_ID)));
            result.put(DAN_RANK, TekkenDataMapperUtils.getDanName(stats.get(DAN_RANK)));
        }
        return result;
    }

    public Map<String, String> getRecentlyPlayedCharacter() {
        Map<String, String> result = new HashMap<>();
        if (characterStats == null || characterStats.isEmpty()) {
            result.put(CHARACTER_NAME, "No Character Data");
            result.put(DAN_RANK, "N/A");
            return result;
        }

        // Find the character stats with the latest battle timestamp
        Optional<Map.Entry<CharacterStatsId, CharacterStats>> latestCharacterStats = characterStats.entrySet().stream()
                .max(Comparator.comparingLong(entry -> entry.getValue().getLatestBattle()));

        String characterId = latestCharacterStats.get().getKey().getCharacterId();
        int danRankId = latestCharacterStats.get().getValue().getDanRank();
        result.put(CHARACTER_NAME, TekkenDataMapperUtils.getCharacterName(characterId));
        result.put(DAN_RANK, TekkenDataMapperUtils.getDanName(String.valueOf(danRankId)));

        return result;
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

    @Override
    public String toString() {
        return "Player: " + playerId;
    }
}
