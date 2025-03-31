package org.ewgf.services;

import org.ewgf.dtos.*;
import org.ewgf.interfaces.BattlesProjection;
import org.ewgf.models.CharacterStatsId;
import org.ewgf.models.Player;
import org.ewgf.repositories.BattleRepository;
import org.ewgf.repositories.PlayerRepository;
import org.ewgf.utils.DateTimeUtils;
import org.ewgf.utils.TekkenDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.ewgf.utils.Constants.CHARACTER_NAME;
import static org.ewgf.utils.Constants.DAN_RANK;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final BattleRepository battleRepository;
    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    public PlayerService(PlayerRepository playerRepository, BattleRepository battleRepository) {
        this.playerRepository = playerRepository;
        this.battleRepository = battleRepository;
    }

    public PlayerDTO getPlayerStats(String polarisId) {
        Optional<Player> playerStats = playerRepository.findByPolarisId(polarisId);
        if (playerStats.isEmpty()) return null;

        Optional<List<BattlesProjection>> playerBattles =
                battleRepository.findAllBattlesByPlayer(playerStats.get().getPlayerId());

        return convertToPlayerDTO(playerStats.get(),
                playerBattles.orElse(Collections.emptyList()));
    }

    public List<PlayerSearchDTO> searchPlayers(String query) {
        if (query.isEmpty() || query.length() >= 20) return Collections.emptyList();
        Optional<List<Player>> playersOpt = playerRepository.findByNameOrPolarisIdContainingIgnoreCase(query);

        return playersOpt.map(players -> players.stream()
                .map(this::convertToSearchDTO)
                .toList())
                .orElse(Collections.emptyList());
    }

    public PlayerMetadataDTO getPlayerMetadata(String polarisId) {
        Optional<Player> player = playerRepository.findByPolarisId(polarisId);
        return player.map(this::convertToMetadataDTO).orElse(null);
    }

    private PlayerDTO convertToPlayerDTO(Player player, List<BattlesProjection> playerBattles) {
        PlayerDTO dto = new PlayerDTO(
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

        if (playerBattles != null && !playerBattles.isEmpty()) {
            List<BattleDTO> battleDTOs = playerBattles.stream()
                    .map(battle -> new BattleDTO(
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

    private PlayerSearchDTO convertToSearchDTO(Player player) {
        PlayerSearchDTO dto = new PlayerSearchDTO();
        Map<String, String> characterInfo = player.getMostPlayedCharacterInfo();
        dto.setId(player.getPlayerId());
        dto.setName(player.getName());
        dto.setTekkenId(player.getPolarisId());
        dto.setRegionId(player.getRegionId() == null ? -1 : player.getRegionId());
        dto.setMostPlayedCharacter(characterInfo.get(CHARACTER_NAME));
        dto.setDanRankName(characterInfo.get(DAN_RANK));
        return dto;
    }

    public List<RecentlyActivePlayersDTO> getRecentlyActivePlayers() {
        Optional<List<Player>> recentlyActivePlayers = playerRepository.findAllActivePlayersInLast10Minutes();
        if (recentlyActivePlayers.isEmpty()) return Collections.emptyList();
        List<RecentlyActivePlayersDTO> recentlyActivePlayersDTOs = new ArrayList<>();

        for (Player player : recentlyActivePlayers.get()) {
            RecentlyActivePlayersDTO dto = new RecentlyActivePlayersDTO();
            dto.setName(player.getName());
            dto.setTekkenPower(player.getTekkenPower());
            dto.setPolarisId(player.getPolarisId());
            dto.setRegion(player.getRegionId());
            dto.setCharacterAndRank(player.getRecentlyPlayedCharacter());
            dto.setLastSeen(player.getLatestBattle());
            recentlyActivePlayersDTOs.add(dto);
        }
        return recentlyActivePlayersDTOs;
    }
}