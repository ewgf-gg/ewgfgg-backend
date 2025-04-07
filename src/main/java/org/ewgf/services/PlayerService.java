package org.ewgf.services;

import org.ewgf.dtos.*;
import org.ewgf.dtos.header_or_footer.PlayerSearchDTO;
import org.ewgf.dtos.homepage.RegionalPlayerDistributionDTO;
import org.ewgf.dtos.player_stats_page.*;
import org.ewgf.interfaces.RegionalPlayerDistributionProjection;
import org.ewgf.models.Battle;
import org.ewgf.models.CharacterStats;
import org.ewgf.models.CharacterStatsId;
import org.ewgf.models.Player;
import org.ewgf.repositories.BattleRepository;
import org.ewgf.repositories.PlayerRepository;
import org.ewgf.utils.DateTimeUtils;
import org.ewgf.utils.TekkenDataMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.ewgf.utils.Constants.*;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final BattleRepository battleRepository;
    private static final int MINIMUM_GAMES = 3;
    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    public PlayerService(PlayerRepository playerRepository, BattleRepository battleRepository) {
        this.playerRepository = playerRepository;
        this.battleRepository = battleRepository;
    }

    public PlayerDTO getPlayerStats(String polarisId) {
        Optional<Player> playerStats = playerRepository.findByPolarisId(polarisId);
        if (playerStats.isEmpty()) return null;

        Optional<List<Battle>> playerBattles =
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

    private PlayerDTO convertToPlayerDTO(Player player, List<Battle> playerBattles) {
        PlayerDTO playerDto = new PlayerDTO();
        playerDto.setPolarisId(player.getPolarisId());
        playerDto.setName(player.getName());
        playerDto.setRegionId(player.getRegionId());
        playerDto.setAreaId(player.getAreaId());
        playerDto.setTekkenPower(player.getTekkenPower());
        playerDto.setLatestBattle(player.getLatestBattle());
        playerDto.setMainCharacterAndRank(player.getMostPlayedCharacterInfo());
        Map<String, PlayerMatchupSummaryDTO> matchupSummaryDto = initializePlayerMatchupSummaryDTO(player.getCharacterStats());
        playerDto.setPlayedCharacters(matchupSummaryDto);

        for (Battle battle : playerBattles) {
            updatePlayerDTOWithBattle(playerDto, battle);
            BattleDTO battleDTO = new BattleDTO( battle.getDate(),
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
                    battle.getStageId());
            playerDto.getBattles().add(battleDTO);
        }
        return playerDto;
    }

    private void updatePlayerDTOWithBattle(PlayerDTO player, Battle battle) {
        String characterPlayedByPlayer = getPlayerCharacter(player, battle);
        String characterPlayedByOpponent = getOpponentCharacter(player, battle);
        Integer playerNumber = getPlayerNumber(player, battle);
        PlayerMatchupSummaryDTO matchupSummary = player.getPlayedCharacters().get(characterPlayedByPlayer);
        MatchupStat matchupStat = matchupSummary.getMatchups().getOrDefault(characterPlayedByOpponent, new MatchupStat());

        if(isWinner(playerNumber, battle)){
            matchupStat.incrementWins();
        }
        else {
            matchupStat.incrementLosses();
        }
        matchupSummary.getMatchups().put(characterPlayedByOpponent, matchupStat);
        updateBestAndWorstMatchups(matchupSummary);

    }

    private Map<String, PlayerMatchupSummaryDTO> initializePlayerMatchupSummaryDTO(Map<CharacterStatsId, CharacterStats> characterStats) {
        Map<String, PlayerMatchupSummaryDTO> allCharacterMatchups = new HashMap<>();
        Map<String, Integer> latestCurrentSeasonVersion = new HashMap<>();
        Map<String, Integer> latestPreviousSeasonVersion = new HashMap<>();
        Map<String, Integer> currentSeasonDanRanks = new HashMap<>();
        Map<String, Integer> previousSeasonDanRanks = new HashMap<>();

        for (Map.Entry<CharacterStatsId, CharacterStats> entry : characterStats.entrySet()) {
            CharacterStatsId id = entry.getKey();
            CharacterStats stats = entry.getValue();
            String characterName = TekkenDataMapperUtils.getCharacterName(id.getCharacterId());

            if (id.getGameVersion() >= SEASON_2_GAME_VERSION) {
                Integer latestVersion = latestCurrentSeasonVersion.getOrDefault(characterName, -1);
                if (id.getGameVersion() > latestVersion) {
                    latestCurrentSeasonVersion.put(characterName, id.getGameVersion());
                    currentSeasonDanRanks.put(characterName, stats.getDanRank());
                }
            } else {
                // Previous season
                Integer latestVersion = latestPreviousSeasonVersion.getOrDefault(characterName, -1);
                if (id.getGameVersion() > latestVersion) {
                    latestPreviousSeasonVersion.put(characterName, id.getGameVersion());
                    previousSeasonDanRanks.put(characterName, stats.getDanRank());
                }
            }
        }

        for (Map.Entry<CharacterStatsId, CharacterStats> entry : characterStats.entrySet()) {
            CharacterStatsId id = entry.getKey();
            CharacterStats stats = entry.getValue();
            String characterName = TekkenDataMapperUtils.getCharacterName(id.getCharacterId());

            PlayerMatchupSummaryDTO currentCharacter = allCharacterMatchups.getOrDefault(characterName, new PlayerMatchupSummaryDTO());

            currentCharacter.setWins(currentCharacter.getWins() + stats.getWins());
            currentCharacter.setLosses(currentCharacter.getLosses() + stats.getLosses());

            if (currentCharacter.getWins() + currentCharacter.getLosses() > 0) {
                currentCharacter.setCharacterWinrate(((float) currentCharacter.getWins() /
                        (currentCharacter.getWins() + currentCharacter.getLosses())) * 100);
            }

            currentCharacter.setCurrentSeasonDanRank(currentSeasonDanRanks.get(characterName));
            currentCharacter.setPreviousSeasonDanRank(previousSeasonDanRanks.get(characterName));
            allCharacterMatchups.put(characterName, currentCharacter);
        }

        return allCharacterMatchups;
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
        dto.setFormattedTekkenId(formatPolarisId(player.getPolarisId()));
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

    public RegionalPlayerDistributionDTO getRegionalPlayerDistribution() {
        RegionalPlayerDistributionProjection projection = playerRepository.findAllPlayerCountByRegion();
        return new RegionalPlayerDistributionDTO(
                projection.getAsia(),
                projection.getEurope(),
                projection.getAmericas(),
                projection.getOceania(),
                projection.getMiddleEast(),
                projection.getUnassigned()
        );
    }

    private void updateBestAndWorstMatchups(PlayerMatchupSummaryDTO matchupSummary) {
        Float bestWinRate = null;
        Float worstWinRate = null;
        String bestCharacter = null;
        String worstCharacter = null;

        for (Map.Entry<String, MatchupStat> entry : matchupSummary.getMatchups().entrySet()) {
            String character = entry.getKey();
            MatchupStat stat = entry.getValue();

            if (stat.getTotalMatches() < MINIMUM_GAMES) {
                continue;
            }
            if (bestWinRate == null || stat.getWinRate() > bestWinRate) {
                bestWinRate = stat.getWinRate();
                bestCharacter = character;
            }
            if (worstWinRate == null || stat.getWinRate() < worstWinRate) {
                worstWinRate = stat.getWinRate();
                worstCharacter = character;
            }
        }

        if (bestCharacter != null) {
            matchupSummary.getBestMatchup().clear();
            matchupSummary.getBestMatchup().put(bestCharacter, bestWinRate);
        }

        if (worstCharacter != null) {
            matchupSummary.getWorstMatchup().clear();
            matchupSummary.getWorstMatchup().put(worstCharacter, worstWinRate);
        }
    }

    private String getOpponentCharacter(PlayerDTO playerDto, Battle battle) {
        String characterId = playerDto.getPolarisId().equals(battle.getPlayer1PolarisId())
                ? String.valueOf(battle.getPlayer2CharacterId())
                : String.valueOf(battle.getPlayer1CharacterId());
        return TekkenDataMapperUtils.getCharacterName(characterId);
    }

    private String getPlayerCharacter(PlayerDTO playerDto, Battle battle) {
        String characterId = playerDto.getPolarisId().equals(battle.getPlayer1PolarisId())
                ? String.valueOf(battle.getPlayer1CharacterId())
                : String.valueOf(battle.getPlayer2CharacterId());
        return TekkenDataMapperUtils.getCharacterName(characterId);
    }

    private boolean isWinner(Integer playerNumber, Battle battle) {
        return playerNumber == battle.getWinner();
    }

    private Integer getPlayerNumber(PlayerDTO playerDto, Battle battle) {
        return playerDto.getPolarisId().equals(battle.getPlayer1PolarisId()) ? 1 : 2;
    }

    private String formatPolarisId(String rawPolarisId) {
        if (rawPolarisId == null || rawPolarisId.length() < 12) return rawPolarisId;
        try {
            StringBuilder formatted = new StringBuilder(rawPolarisId);
            formatted.insert(4, '-');
            formatted.insert(9, '-');
            return formatted.toString();
        } catch (Exception e) {
            logger.error("Exception thrown while formatting polarisId, returning unformatted version. Exception: {}", e.getMessage());
            return rawPolarisId;
        }
    }
}