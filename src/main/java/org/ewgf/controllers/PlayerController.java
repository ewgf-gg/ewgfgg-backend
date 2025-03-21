package org.ewgf.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.ewgf.dtos.*;
import org.ewgf.utils.DateTimeUtils;
import org.ewgf.utils.TekkenDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ewgf.interfaces.BattlesProjection;
import org.ewgf.models.CharacterStatsId;
import org.ewgf.models.Player;
import org.ewgf.repositories.BattleRepository;
import org.ewgf.repositories.PlayerRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/player-stats")
public class PlayerController {
    private final PlayerRepository playerRepository;
    private final BattleRepository battleRepository;

    public PlayerController(PlayerRepository playerRepository, BattleRepository battleRepository) {
        this.playerRepository = playerRepository;
        this.battleRepository = battleRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    @GetMapping("/{player}")
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

    @GetMapping("/metaData/{polarisId}")
    public ResponseEntity<PlayerMetadataDTO> getPlayerMetadata(@PathVariable String polarisId) {
        Optional<Player> player = playerRepository.findByPolarisId(polarisId);

        return player.map(value -> ResponseEntity.ok(convertToMetadataDTO(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
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

        dto.setMainCharacterAndRank(player.getMostPlayedCharacterInfo());

        // Convert character stats
        Map<CharacterStatsId, CharacterStatsDTO> characterStatsMap = new HashMap<>();
        if (player.getCharacterStats() != null) {
            player.getCharacterStats().forEach((characterStatsId, characterStats) -> {
                CharacterStatsDTO characterStatsDTO = new CharacterStatsDTO(
                        TekkenDataMapper.getCharacterName(characterStatsId.getCharacterId()),
                        TekkenDataMapper.getDanName(String.valueOf(characterStats.getDanRank())),
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

    private PlayerMetadataDTO convertToMetadataDTO(Player player) {
        PlayerMetadataDTO dto = new PlayerMetadataDTO();

        dto.setPlayerName(player.getName());
        dto.setRegionId(player.getRegionId());
        dto.setAreaId(player.getAreaId());
        dto.setPolarisId(player.getPolarisId());
        dto.setLatestBattleDate(DateTimeUtils.toReadableTime(player.getLatestBattle()));
        dto.setTekkenPower(player.getTekkenPower());
        dto.setMainCharacterAndRank(player.getMostPlayedCharacterInfo());
        return dto;
    }

    private PlayerSearchDTO convertToSearchDTO(Player player)
    {
        PlayerSearchDTO dto = new PlayerSearchDTO();
        Map<String, String> characterInfo = player.getMostPlayedCharacterInfo();

        dto.setId(player.getPlayerId());
        dto.setName(player.getName());
        dto.setTekkenId(player.getPolarisId());
        dto.setRegionId(player.getRegionId() == null ? -1 : player.getRegionId());
        dto.setMostPlayedCharacter(characterInfo.get("characterName"));
        dto.setDanRankName(characterInfo.get("danRank"));

        return dto;
    }
}
