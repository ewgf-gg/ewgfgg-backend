package org.tekkenstats.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tekkenstats.dtos.CharacterStatsDTO;
import org.tekkenstats.dtos.PlayerBattleDTO;
import org.tekkenstats.dtos.PlayerSearchDTO;
import org.tekkenstats.dtos.PlayerStatsDTO;
import org.tekkenstats.interfaces.PlayerWithBattlesProjection;
import org.tekkenstats.mappers.enumsMapper;
import org.tekkenstats.models.CharacterStats;
import org.tekkenstats.models.CharacterStatsId;
import org.tekkenstats.models.Player;
import org.tekkenstats.repositories.PlayerRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/player-stats")
public class PlayerController {
    private final enumsMapper enumsMapper;
    private final PlayerRepository playerRepository;

    public PlayerController(enumsMapper enumsMapper, PlayerRepository playerRepository) {
        this.enumsMapper = enumsMapper;
        this.playerRepository = playerRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    @GetMapping("/{player}")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(@PathVariable String player, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        logger.info("Received request for Player: {} from IP: {}", player, clientIp);

        List<PlayerWithBattlesProjection> results = playerRepository.findPlayerWithBattlesAndStats(player);

        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PlayerStatsDTO playerStats = convertToPlayerStatsDTO(results);
        return ResponseEntity.ok(playerStats);
    }

    @GetMapping("/search")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<List<PlayerSearchDTO>> searchPlayers(@RequestParam String query) {
        if (query.isEmpty() || query.length() >= 20) {
            return ResponseEntity.badRequest().build();
        }

        Optional<List<Player>> playersOpt = playerRepository.findByNameOrPolarisIdContainingIgnoreCase(query);

        if (playersOpt.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<PlayerSearchDTO> projections = playersOpt.get().stream()
                .map(this::convertToSearchDTO)
                .limit(20)
                .collect(Collectors.toList());

        return ResponseEntity.ok(projections);
    }

    private PlayerStatsDTO convertToPlayerStatsDTO(List<PlayerWithBattlesProjection> projections)
    {
        PlayerWithBattlesProjection firstResult = projections.getFirst();

        // Create base player data
        Player player = createPlayerFromProjection(firstResult);

        // Create the DTO and set basic fields
        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.setPlayerId(player.getPlayerId());
        dto.setName(player.getName());
        dto.setRegionId(player.getRegionId());
        dto.setAreaId(player.getAreaId());
        dto.setTekkenPower(player.getTekkenPower());
        dto.setLatestBattle(player.getLatestBattle());

        // Add character stats and battles
        dto.setCharacterStats(createCharacterStatsMap(projections));
        dto.setBattles(createBattlesList(projections));

        return dto;
    }

    private Player createPlayerFromProjection(PlayerWithBattlesProjection projection) {
        return new Player(
                projection.getPlayerId(),
                projection.getName(),
                projection.getPolarisId(),
                projection.getTekkenPower(),
                projection.getRegionId(),
                projection.getAreaId(),
                projection.getLanguage(),
                projection.getLatestBattle()
        );
    }

    private Map<CharacterStatsId, CharacterStatsDTO> createCharacterStatsMap(List<PlayerWithBattlesProjection> projections) {
        Map<CharacterStatsId, CharacterStatsDTO> statsMap = new HashMap<>();
        String playerId = projections.getFirst().getPlayerId();

        for (PlayerWithBattlesProjection proj : projections) {
            if (proj.getCharacterId() == null) {
                continue;
            }

            // Create ID only once per iteration
            CharacterStatsId id = new CharacterStatsId();
            id.setPlayerId(playerId);
            id.setCharacterId(proj.getCharacterId());
            id.setGameVersion(proj.getGameVersion());

            // Skip if we already have this character's stats
            if (statsMap.containsKey(id)) {
                continue;
            }

            // Create DTO
            CharacterStatsDTO dto = new CharacterStatsDTO();
            dto.setCharacterName(enumsMapper.getCharacterName(proj.getCharacterId()));
            dto.setDanName(enumsMapper.getDanName(String.valueOf(proj.getDanRank())));
            dto.setDanRank(proj.getDanRank());
            dto.setWins(proj.getWins());
            dto.setLosses(proj.getLosses());

            statsMap.put(id, dto);
        }

        return statsMap;
    }
    private CharacterStatsId createCharacterStatsId(PlayerWithBattlesProjection projection) {
        CharacterStatsId id = new CharacterStatsId();
        id.setPlayerId(projection.getPlayerId());
        id.setCharacterId(projection.getCharacterId());
        id.setGameVersion(projection.getGameVersion());
        return id;
    }

    private CharacterStatsDTO createCharacterStatsDTO(PlayerWithBattlesProjection projection) {
        CharacterStatsDTO dto = new CharacterStatsDTO();
        dto.setCharacterName(enumsMapper.getCharacterName(projection.getCharacterId()));
        dto.setDanName(enumsMapper.getDanName(String.valueOf(projection.getDanRank())));
        dto.setDanRank(projection.getDanRank());
        dto.setWins(projection.getWins());
        dto.setLosses(projection.getLosses());
        return dto;
    }

    private List<PlayerBattleDTO> createBattlesList(List<PlayerWithBattlesProjection> projections) {
        List<PlayerBattleDTO> battles = new ArrayList<>();
        Set<String> processedBattles = new HashSet<>();  // To handle distinct battles

        for (PlayerWithBattlesProjection proj : projections) {


            // Create a unique key for this battle to check distinctness
            String battleKey = proj.getDate() + proj.getPlayer1Name() + proj.getPlayer2Name();
            if (processedBattles.contains(battleKey)) {
                continue;
            }
            processedBattles.add(battleKey);

            PlayerBattleDTO battle = new PlayerBattleDTO();
            battle.setDate(proj.getDate());
            battle.setPlayer1Name(proj.getPlayer1Name());
            battle.setPlayer1CharacterId(proj.getPlayer1CharacterId());
            battle.setPlayer1RegionId(proj.getPlayer1RegionId());
            battle.setPlayer1DanRank(proj.getPlayer1DanRank());
            battle.setPlayer2Name(proj.getPlayer2Name());
            battle.setPlayer2RegionId(proj.getPlayer2RegionId());
            battle.setPlayer2CharacterId(proj.getPlayer2CharacterId());
            battle.setPlayer2DanRank(proj.getPlayer2DanRank());
            battle.setPlayer1RoundsWon(proj.getPlayer1RoundsWon());
            battle.setPlayer2RoundsWon(proj.getPlayer2RoundsWon());
            battle.setWinner(proj.getWinner());
            battle.setStageId(proj.getStageId());

            battles.add(battle);
        }

        return battles;
    }

    private PlayerBattleDTO createBattleDTO(PlayerWithBattlesProjection projection)
    {
        PlayerBattleDTO battle = new PlayerBattleDTO();
        battle.setDate(projection.getDate());
        battle.setPlayer1Name(projection.getPlayer1Name());
        battle.setPlayer1CharacterId(projection.getPlayer1CharacterId());
        battle.setPlayer1RegionId(projection.getPlayer1RegionId());
        battle.setPlayer1DanRank(projection.getPlayer1DanRank());
        battle.setPlayer2Name(projection.getPlayer2Name());
        battle.setPlayer2CharacterId(projection.getPlayer2CharacterId());
        battle.setPlayer2RegionId(projection.getPlayer2RegionId());
        battle.setPlayer2DanRank(projection.getPlayer2DanRank());
        battle.setPlayer1RoundsWon(projection.getPlayer1RoundsWon());
        battle.setPlayer2RoundsWon(projection.getPlayer2RoundsWon());
        battle.setWinner(projection.getWinner());
        battle.setStageId(projection.getStageId());
        return battle;
    }

    private PlayerSearchDTO convertToSearchDTO(Player player) {
        PlayerSearchDTO dto = new PlayerSearchDTO();
        Map<String, String> characterInfo = player.getMostPlayedCharacterInfo(enumsMapper);

        dto.setId(player.getPlayerId());
        dto.setName(player.getName());
        dto.setTekkenId(player.getPolarisId());
        dto.setRegionId(player.getRegionId() == null ? -1 : player.getRegionId());
        dto.setMostPlayedCharacter(characterInfo.get("characterName"));
        dto.setDanRankName(characterInfo.get("danRank"));

        return dto;
    }

    private CharacterStatsDTO convertToCharacterStatsDTO(CharacterStatsId id, CharacterStats stats) {
        CharacterStatsDTO characterStatsDTO = new CharacterStatsDTO();
        characterStatsDTO.setCharacterName(enumsMapper.getCharacterName(id.getCharacterId()));
        characterStatsDTO.setDanName(enumsMapper.getDanName(Integer.toString(stats.getDanRank())));
        characterStatsDTO.setDanRank(stats.getDanRank());
        characterStatsDTO.setWins(stats.getWins());
        characterStatsDTO.setLosses(stats.getLosses());
        return characterStatsDTO;
    }
}
