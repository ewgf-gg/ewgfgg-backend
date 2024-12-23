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
import org.tekkenstats.interfaces.BattlesProjection;
import org.tekkenstats.mappers.EnumsMapper;
import org.tekkenstats.models.CharacterStatsId;
import org.tekkenstats.models.Player;
import org.tekkenstats.repositories.BattleRepository;
import org.tekkenstats.repositories.PlayerRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/player-stats")
public class PlayerController {
    private final EnumsMapper enumsMapper;
    private final PlayerRepository playerRepository;
    private final BattleRepository battleRepository;

    public PlayerController(EnumsMapper enumsMapper, PlayerRepository playerRepository, BattleRepository battleRepository) {
        this.enumsMapper = enumsMapper;
        this.playerRepository = playerRepository;
        this.battleRepository = battleRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    @GetMapping("/{player}")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(@PathVariable String player, HttpServletRequest request)
    {
        String clientIp = request.getRemoteAddr();
        logger.info("Received request for Player: {} from IP: {}", player, clientIp);

        Optional<Player> playerStats = playerRepository.findByPolarisId(player);
        Optional<List<BattlesProjection>> playerBattles = battleRepository.findAllBattlesByPlayer(playerStats.get().getPlayerId());

        PlayerStatsDTO playerStatsDTO = convertToPlayerDTO(playerStats.get(), playerBattles.get());
        logger.info("Request from client: {} completed successfully.", clientIp);
        return ResponseEntity.ok(playerStatsDTO);
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
                .collect(Collectors.toList());

        return ResponseEntity.ok(projections);
    }

    private PlayerStatsDTO convertToPlayerDTO(Player player, List<BattlesProjection> playerBattles) {
        // Create new PlayerStatsDTO with basic player information
        PlayerStatsDTO dto = new PlayerStatsDTO(
                player.getPlayerId(),
                player.getName(),
                player.getRegionId(),
                player.getAreaId(),
                player.getTekkenPower(),
                player.getLatestBattle()
        );

        dto.setMainCharacterAndRank(player.getMostPlayedCharacterInfo(enumsMapper));

        // Convert character stats
        Map<CharacterStatsId, CharacterStatsDTO> characterStatsMap = new HashMap<>();
        if (player.getCharacterStats() != null) {
            player.getCharacterStats().forEach((characterStatsId, characterStats) -> {
                CharacterStatsDTO characterStatsDTO = new CharacterStatsDTO(
                        enumsMapper.getCharacterName(characterStatsId.getCharacterId()),
                        enumsMapper.getDanName(String.valueOf(characterStats.getDanRank())),
                        characterStats.getDanRank(),
                        characterStats.getWins(),
                        characterStats.getLosses()
                );
                characterStatsMap.put(characterStatsId, characterStatsDTO);
            });
        }
        dto.setCharacterStats(characterStatsMap);

        // Convert battles to DTOs
        if (playerBattles != null && !playerBattles.isEmpty()) {
            List<PlayerBattleDTO> battleDTOs = playerBattles.stream()
                    .map(battle -> new PlayerBattleDTO(
                            battle.getDate(),
                            battle.getGameVersion(),
                            battle.getPlayer1Name(),
                            battle.getPlayer1PolarisId(),
                            battle.getPlayer1CharacterId(),
                            battle.getPlayer1RegionId(),
                            battle.getPlayer1TekkenPower(),
                            battle.getPlayer1DanRank(),
                            battle.getPlayer2Name(),
                            battle.getPlayer2PolarisId(),
                            battle.getPlayer2RegionId(),
                            battle.getPlayer2CharacterId(),
                            battle.getPlayer2DanRank(),
                            battle.getPlayer2TekkenPower(),
                            battle.getPlayer1RoundsWon(),
                            battle.getPlayer2RoundsWon(),
                            battle.getWinner(),
                            battle.getStageId()
                    ))
                    .collect(Collectors.toList());
            dto.setBattles(battleDTOs);
        }
        return dto;
    }

    private PlayerSearchDTO convertToSearchDTO(Player player)
    {
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
}
