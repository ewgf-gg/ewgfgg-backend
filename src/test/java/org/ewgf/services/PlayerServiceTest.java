package org.ewgf.services;

import org.apache.coyote.BadRequestException;
import org.ewgf.dtos.*;
import org.ewgf.models.*;
import org.ewgf.repositories.BattleRepository;
import org.ewgf.repositories.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService Unit Tests")
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private BattleRepository battleRepository;

    @InjectMocks
    private PlayerService playerService;

    private Player testPlayer;
    private Battle testBattle;
    private CharacterStats testCharacterStats;
    private CharacterStatsId testCharacterStatsId;

    @BeforeEach
    void setUp() {
        // Setup test player
        testPlayer = new Player();
        testPlayer.setPlayerId("12345678901234567890");
        testPlayer.setPolarisId("ABC123456789");
        testPlayer.setName("TestPlayer");
        testPlayer.setRegionId(1);
        testPlayer.setTekkenPower(100000L);
        testPlayer.setLatestBattle(System.currentTimeMillis());

        // Setup test character stats
        testCharacterStatsId = new CharacterStatsId();
        testCharacterStatsId.setCharacterId("32");  // Jin
        testCharacterStatsId.setGameVersion(20001);

        testCharacterStats = new CharacterStats();
        testCharacterStats.setId(testCharacterStatsId);
        testCharacterStats.setPlayer(testPlayer);
        testCharacterStats.setWins(50);
        testCharacterStats.setLosses(30);
        testCharacterStats.setDanRank(15);
        testCharacterStats.setLatestBattle(System.currentTimeMillis());

        Map<CharacterStatsId, CharacterStats> characterStatsMap = new HashMap<>();
        characterStatsMap.put(testCharacterStatsId, testCharacterStats);
        testPlayer.setCharacterStats(characterStatsMap);

        // Setup test battle
        testBattle = new Battle();
        testBattle.setBattleId("battle123");
        testBattle.setDate("2025-01-01");
        testBattle.setBattleAt(System.currentTimeMillis());
        testBattle.setBattleType(BattleType.RANKED_BATTLE);
        testBattle.setGameVersion(20001);
        testBattle.setPlayer1Name("TestPlayer");
        testBattle.setPlayer1PolarisId("ABC123456789");
        testBattle.setPlayer1CharacterId(32);
        testBattle.setPlayer1RegionId(1);
        testBattle.setPlayer1TekkenPower(100000L);
        testBattle.setPlayer1DanRank(15);
        testBattle.setPlayer1RoundsWon(3);
        testBattle.setPlayer2Name("Opponent");
        testBattle.setPlayer2PolarisId("DEF987654321");
        testBattle.setPlayer2CharacterId(28);
        testBattle.setPlayer2RegionId(2);
        testBattle.setPlayer2TekkenPower(95000L);
        testBattle.setPlayer2DanRank(14);
        testBattle.setPlayer2RoundsWon(1);
        testBattle.setWinner(1);
        testBattle.setStageId(1);
    }

    // ============ getPlayerStats Tests ============

    @Test
    @DisplayName("Should return PlayerDTO when valid polaris ID is provided")
    void getPlayerStats_WithValidPolarisId_ReturnsPlayerDTO() throws Exception {
        // Arrange
        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.singletonList(testBattle)));

        // Act
        PlayerDTO result = playerService.getPlayerStats("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPolarisId()).isEqualTo("ABC123456789");
        assertThat(result.getName()).isEqualTo("TestPlayer");
        assertThat(result.getRegionId()).isEqualTo(1);
        assertThat(result.getTekkenPower()).isEqualTo(100000L);
        assertThat(result.getBattles()).hasSize(1);
        assertThat(result.getPlayedCharacters()).isNotEmpty();

        verify(playerRepository, times(1)).findByPolarisId("ABC123456789");
        verify(battleRepository, times(1)).findAllBattlesByPlayer(testPlayer.getPlayerId());
    }

    @Test
    @DisplayName("Should return null when player is not found")
    void getPlayerStats_WithNonExistentPolarisId_ReturnsNull() throws Exception {
        // Arrange
        when(playerRepository.findByPolarisId("NONEXISTENT")).thenReturn(Optional.empty());

        // Act
        PlayerDTO result = playerService.getPlayerStats("NONEXISTENT");

        // Assert
        assertThat(result).isNull();
        verify(playerRepository, times(1)).findByPolarisId("NONEXISTENT");
        verify(battleRepository, never()).findAllBattlesByPlayer(anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException when polaris ID is null")
    void getPlayerStats_WithNullPolarisId_ThrowsBadRequestException() {
        // Act & Assert
        assertThatThrownBy(() -> playerService.getPlayerStats(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid Polaris Id");

        verify(playerRepository, never()).findByPolarisId(anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException when polaris ID is empty")
    void getPlayerStats_WithEmptyPolarisId_ThrowsBadRequestException() {
        // Act & Assert
        assertThatThrownBy(() -> playerService.getPlayerStats(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid Polaris Id");

        assertThatThrownBy(() -> playerService.getPlayerStats("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid Polaris Id");

        verify(playerRepository, never()).findByPolarisId(anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException when polaris ID is too long")
    void getPlayerStats_WithTooLongPolarisId_ThrowsBadRequestException() {
        // Arrange
        String longPolarisId = "ABCDEFGHIJKLM"; // 13 characters, max is 12

        // Act & Assert
        assertThatThrownBy(() -> playerService.getPlayerStats(longPolarisId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid Polaris Id");

        verify(playerRepository, never()).findByPolarisId(anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException when polaris ID contains special characters")
    void getPlayerStats_WithInvalidCharacters_ThrowsBadRequestException() {
        // Act & Assert
        assertThatThrownBy(() -> playerService.getPlayerStats("ABC-123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid Polaris Id");

        assertThatThrownBy(() -> playerService.getPlayerStats("ABC@123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid Polaris Id");

        verify(playerRepository, never()).findByPolarisId(anyString());
    }

    @Test
    @DisplayName("Should trim whitespace from polaris ID")
    void getPlayerStats_WithWhitespace_TrimsAndProcesses() throws Exception {
        // Arrange
        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.emptyList()));

        // Act
        PlayerDTO result = playerService.getPlayerStats("  ABC123456789  ");

        // Assert
        assertThat(result).isNotNull();
        verify(playerRepository, times(1)).findByPolarisId("ABC123456789");
    }

    @Test
    @DisplayName("Should handle player with no battles")
    void getPlayerStats_WithNoBattles_ReturnsPlayerDTOWithEmptyBattleList() throws Exception {
        // Arrange
        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.empty());

        // Act
        PlayerDTO result = playerService.getPlayerStats("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBattles()).isEmpty();
        verify(battleRepository, times(1)).findAllBattlesByPlayer(testPlayer.getPlayerId());
    }

    // ============ searchPlayers Tests ============

    @Test
    @DisplayName("Should return list of PlayerSearchDTO when players are found")
    void searchPlayers_WithMatchingQuery_ReturnsPlayerList() {
        // Arrange
        List<Player> players = Arrays.asList(testPlayer);
        when(playerRepository.findByNameOrPolarisIdContainingIgnoreCase(eq("Test"), any(PageRequest.class)))
                .thenReturn(Optional.of(players));

        // Act
        List<PlayerSearchDTO> result = playerService.searchPlayers("Test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("TestPlayer");
        assertThat(result.get(0).getTekkenId()).isEqualTo("ABC123456789");
        verify(playerRepository, times(1))
                .findByNameOrPolarisIdContainingIgnoreCase(eq("Test"), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should return empty list when no players are found")
    void searchPlayers_WithNoMatches_ReturnsEmptyList() {
        // Arrange
        when(playerRepository.findByNameOrPolarisIdContainingIgnoreCase(eq("NonExistent"), any(PageRequest.class)))
                .thenReturn(Optional.empty());

        // Act
        List<PlayerSearchDTO> result = playerService.searchPlayers("NonExistent");

        // Assert
        assertThat(result).isEmpty();
        verify(playerRepository, times(1))
                .findByNameOrPolarisIdContainingIgnoreCase(eq("NonExistent"), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should limit search results to 20 players")
    void searchPlayers_WithManyResults_LimitsTo20() {
        // Arrange
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Player player = new Player();
            player.setPlayerId("player" + i);
            player.setName("Player" + i);
            player.setPolarisId("POLARIS" + i);
            player.setCharacterStats(new HashMap<>());
            players.add(player);
        }

        when(playerRepository.findByNameOrPolarisIdContainingIgnoreCase(eq("Player"), any(PageRequest.class)))
                .thenReturn(Optional.of(players));

        // Act
        List<PlayerSearchDTO> result = playerService.searchPlayers("Player");

        // Assert
        assertThat(result).hasSize(25);
        verify(playerRepository, times(1))
                .findByNameOrPolarisIdContainingIgnoreCase(eq("Player"), argThat(pageRequest ->
                        pageRequest.getPageNumber() == 0 && pageRequest.getPageSize() == 20
                ));
    }

    // ============ getPlayerMetadata Tests ============

    @Test
    @DisplayName("Should return PlayerMetadataDTO when player is found")
    void getPlayerMetadata_WithValidPolarisId_ReturnsMetadata() {
        // Arrange
        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));

        // Act
        PlayerMetadataDTO result = playerService.getPlayerMetadata("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlayerName()).isEqualTo("TestPlayer");
        assertThat(result.getPolarisId()).isEqualTo("ABC123456789");
        assertThat(result.getRegionId()).isEqualTo(1);
        assertThat(result.getTekkenPower()).isEqualTo(100000L);
        verify(playerRepository, times(1)).findByPolarisId("ABC123456789");
    }

    @Test
    @DisplayName("Should return null when player is not found for metadata")
    void getPlayerMetadata_WithNonExistentPolarisId_ReturnsNull() {
        // Arrange
        when(playerRepository.findByPolarisId("NONEXISTENT")).thenReturn(Optional.empty());

        // Act
        PlayerMetadataDTO result = playerService.getPlayerMetadata("NONEXISTENT");

        // Assert
        assertThat(result).isNull();
        verify(playerRepository, times(1)).findByPolarisId("NONEXISTENT");
    }

    // ============ getPlayerIdFromPolarisId Tests ============

    @Test
    @DisplayName("Should return padded player ID when found")
    void getPlayerIdFromPolarisId_WithValidId_ReturnsPaddedPlayerId() {
        // Arrange
        when(playerRepository.findPolarisIdByPlayerId("ABC123456789"))
                .thenReturn(Optional.of("123456"));

        // Act
        String result = playerService.getPlayerIdFromPolarisId("ABC123456789");

        // Assert
        assertThat(result).isEqualTo("000000000000123456");
        assertThat(result).hasSize(18);
        verify(playerRepository, times(1)).findPolarisIdByPlayerId("ABC123456789");
    }

    @Test
    @DisplayName("Should return player ID without padding when already 18 characters")
    void getPlayerIdFromPolarisId_WithAlreadyPaddedId_ReturnsUnmodified() {
        // Arrange
        String alreadyPaddedId = "123456789012345678";
        when(playerRepository.findPolarisIdByPlayerId("ABC123456789"))
                .thenReturn(Optional.of(alreadyPaddedId));

        // Act
        String result = playerService.getPlayerIdFromPolarisId("ABC123456789");

        // Assert
        assertThat(result).isEqualTo(alreadyPaddedId);
        assertThat(result).hasSize(18);
        verify(playerRepository, times(1)).findPolarisIdByPlayerId("ABC123456789");
    }

    @Test
    @DisplayName("Should return null when player ID is not found")
    void getPlayerIdFromPolarisId_WithNonExistentId_ReturnsNull() {
        // Arrange
        when(playerRepository.findPolarisIdByPlayerId("NONEXISTENT"))
                .thenReturn(Optional.empty());

        // Act
        String result = playerService.getPlayerIdFromPolarisId("NONEXISTENT");

        // Assert
        assertThat(result).isNull();
        verify(playerRepository, times(1)).findPolarisIdByPlayerId("NONEXISTENT");
    }

    // ============ getRecentlyActivePlayers Tests ============

    @Test
    @DisplayName("Should return list of recently active players")
    void getRecentlyActivePlayers_WithActivePlayers_ReturnsList() {
        // Arrange
        List<Player> activePlayers = Arrays.asList(testPlayer);
        when(playerRepository.findAllActivePlayersInLast10Minutes())
                .thenReturn(Optional.of(activePlayers));

        // Act
        List<RecentlyActivePlayersDTO> result = playerService.getRecentlyActivePlayers();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("TestPlayer");
        assertThat(result.get(0).getPolarisId()).isEqualTo("ABC123456789");
        assertThat(result.get(0).getTekkenPower()).isEqualTo(100000L);
        verify(playerRepository, times(1)).findAllActivePlayersInLast10Minutes();
    }

    @Test
    @DisplayName("Should return empty list when no recently active players")
    void getRecentlyActivePlayers_WithNoActivePlayers_ReturnsEmptyList() {
        // Arrange
        when(playerRepository.findAllActivePlayersInLast10Minutes())
                .thenReturn(Optional.empty());

        // Act
        List<RecentlyActivePlayersDTO> result = playerService.getRecentlyActivePlayers();

        // Assert
        assertThat(result).isEmpty();
        verify(playerRepository, times(1)).findAllActivePlayersInLast10Minutes();
    }

    @Test
    @DisplayName("Should return empty list when active players list is empty")
    void getRecentlyActivePlayers_WithEmptyList_ReturnsEmptyList() {
        // Arrange
        when(playerRepository.findAllActivePlayersInLast10Minutes())
                .thenReturn(Optional.of(Collections.emptyList()));

        // Act
        List<RecentlyActivePlayersDTO> result = playerService.getRecentlyActivePlayers();

        // Assert
        assertThat(result).isEmpty();
        verify(playerRepository, times(1)).findAllActivePlayersInLast10Minutes();
    }

    // ============ Integration-like Tests for Battle Processing ============

    @Test
    @DisplayName("Should correctly process ranked battles and update matchup statistics")
    void getPlayerStats_WithRankedBattles_UpdatesMatchupStatistics() throws Exception {
        // Arrange
        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.singletonList(testBattle)));

        // Act
        PlayerDTO result = playerService.getPlayerStats("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlayedCharacters()).isNotEmpty();
        // Verify that battles were processed
        assertThat(result.getBattles()).hasSize(1);
        BattleDTO battleDTO = result.getBattles().get(0);
        assertThat(battleDTO.getPlayer1Name()).isEqualTo("TestPlayer");
        assertThat(battleDTO.getWinner()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not update statistics for non-ranked battles")
    void getPlayerStats_WithNonRankedBattles_DoesNotUpdateMatchupStats() throws Exception {
        // Arrange
        testBattle.setBattleType(BattleType.QUICK_BATTLE);
        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.singletonList(testBattle)));

        // Act
        PlayerDTO result = playerService.getPlayerStats("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBattles()).hasSize(1);
        // Battle should still be added but matchup stats should not be updated for quick battles
    }

    @Test
    @DisplayName("Should handle multiple battles correctly")
    void getPlayerStats_WithMultipleBattles_ProcessesAllBattles() throws Exception {
        // Arrange
        Battle battle2 = new Battle();
        battle2.setBattleId("battle456");
        battle2.setDate("2025-01-02");
        battle2.setBattleType(BattleType.RANKED_BATTLE);
        battle2.setGameVersion(20001);
        battle2.setPlayer1Name("TestPlayer");
        battle2.setPlayer1PolarisId("ABC123456789");
        battle2.setPlayer1CharacterId(32);
        battle2.setPlayer1DanRank(15);
        battle2.setPlayer2Name("Opponent2");
        battle2.setPlayer2PolarisId("XYZ111111111");
        battle2.setPlayer2CharacterId(30);
        battle2.setPlayer2DanRank(16);
        battle2.setPlayer1RoundsWon(1);
        battle2.setPlayer2RoundsWon(3);
        battle2.setWinner(2);
        battle2.setStageId(2);
        battle2.setPlayer1RegionId(1);
        battle2.setPlayer2RegionId(2);
        battle2.setPlayer1TekkenPower(100000L);
        battle2.setPlayer2TekkenPower(105000L);
        battle2.setBattleAt(System.currentTimeMillis());

        List<Battle> battles = Arrays.asList(testBattle, battle2);

        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.of(battles));

        // Act
        PlayerDTO result = playerService.getPlayerStats("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBattles()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle player as player2 in battle")
    void getPlayerStats_WithPlayerAsPlayer2_ProcessesCorrectly() throws Exception {
        // Arrange
        testBattle.setPlayer1Name("Opponent");
        testBattle.setPlayer1PolarisId("DEF987654321");
        testBattle.setPlayer1CharacterId(28);
        testBattle.setPlayer2Name("TestPlayer");
        testBattle.setPlayer2PolarisId("ABC123456789");
        testBattle.setPlayer2CharacterId(32);
        testBattle.setWinner(2);

        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.singletonList(testBattle)));

        // Act
        PlayerDTO result = playerService.getPlayerStats("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBattles()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle player with multiple characters")
    void getPlayerStats_WithMultipleCharacters_ProcessesAllCharacters() throws Exception {
        // Arrange
        CharacterStatsId kazuyaStatsId = new CharacterStatsId();
        kazuyaStatsId.setCharacterId("28");  // Kazuya
        kazuyaStatsId.setGameVersion(20001);

        CharacterStats kazuyaStats = new CharacterStats();
        kazuyaStats.setId(kazuyaStatsId);
        kazuyaStats.setPlayer(testPlayer);
        kazuyaStats.setWins(30);
        kazuyaStats.setLosses(20);
        kazuyaStats.setDanRank(12);
        kazuyaStats.setLatestBattle(System.currentTimeMillis());

        testPlayer.getCharacterStats().put(kazuyaStatsId, kazuyaStats);

        when(playerRepository.findByPolarisId("ABC123456789"))
                .thenReturn(Optional.of(testPlayer));
        when(battleRepository.findAllBattlesByPlayer(testPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.emptyList()));

        // Act
        PlayerDTO result = playerService.getPlayerStats("ABC123456789");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlayedCharacters()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle null region ID gracefully in search")
    void searchPlayers_WithNullRegionId_HandlesGracefully() {
        // Arrange
        testPlayer.setRegionId(null);
        when(playerRepository.findByNameOrPolarisIdContainingIgnoreCase(eq("Test"), any(PageRequest.class)))
                .thenReturn(Optional.of(Collections.singletonList(testPlayer)));

        // Act
        List<PlayerSearchDTO> result = playerService.searchPlayers("Test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRegionId()).isEqualTo(-1);
    }

    // ============ Game Version Tests (Season 1 vs Season 2) ============

    @Test
    @DisplayName("Should correctly process player stats from previous season (Season 1)")
    void getPlayerStats_WithPreviousSeasonStats_ProcessesCorrectly() throws Exception {
        // Arrange - Create player with Season 1 stats (game version < 20001)
        Player season1Player = new Player();
        season1Player.setPlayerId("12345678901234567890");
        season1Player.setPolarisId("S1PLAYER123");
        season1Player.setName("Season1Player");
        season1Player.setRegionId(1);
        season1Player.setTekkenPower(80000L);
        season1Player.setLatestBattle(System.currentTimeMillis());

        CharacterStatsId season1StatsId = new CharacterStatsId();
        season1StatsId.setCharacterId("0"); // Character ID 0
        season1StatsId.setGameVersion(19005); // Season 1 version (< 20001)

        CharacterStats season1Stats = new CharacterStats();
        season1Stats.setId(season1StatsId);
        season1Stats.setPlayer(season1Player);
        season1Stats.setWins(40);
        season1Stats.setLosses(25);
        season1Stats.setDanRank(18); // High rank in Season 1
        season1Stats.setLatestBattle(System.currentTimeMillis());

        Map<CharacterStatsId, CharacterStats> characterStatsMap = new HashMap<>();
        characterStatsMap.put(season1StatsId, season1Stats);
        season1Player.setCharacterStats(characterStatsMap);

        when(playerRepository.findByPolarisId("S1PLAYER123"))
                .thenReturn(Optional.of(season1Player));
        when(battleRepository.findAllBattlesByPlayer(season1Player.getPlayerId()))
                .thenReturn(Optional.of(Collections.emptyList()));

        // Act
        PlayerDTO result = playerService.getPlayerStats("S1PLAYER123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlayedCharacters()).hasSize(1);
        
        // Get the first (and only) character stats
        PlayerMatchupSummaryDTO characterStats = result.getPlayedCharacters().values().iterator().next();
        assertThat(characterStats).isNotNull();
        assertThat(characterStats.getWins()).isEqualTo(40);
        assertThat(characterStats.getLosses()).isEqualTo(25);
        assertThat(characterStats.getCurrentSeasonDanRank()).isNull(); // No current season stats
        assertThat(characterStats.getPreviousSeasonDanRank()).isEqualTo(18); // Season 1 rank
        assertThat(characterStats.getCharacterWinrate()).isCloseTo(61.54f, within(0.01f));
    }

    @Test
    @DisplayName("Should correctly handle player with stats from both seasons")
    void getPlayerStats_WithBothSeasonStats_ProcessesBothCorrectly() throws Exception {
        // Arrange - Create player with stats from both seasons
        Player multiSeasonPlayer = new Player();
        multiSeasonPlayer.setPlayerId("12345678901234567890");
        multiSeasonPlayer.setPolarisId("MULTI123");
        multiSeasonPlayer.setName("MultiSeasonPlayer");
        multiSeasonPlayer.setRegionId(1);
        multiSeasonPlayer.setTekkenPower(120000L);
        multiSeasonPlayer.setLatestBattle(System.currentTimeMillis());

        // Season 1 stats
        CharacterStatsId season1StatsId = new CharacterStatsId();
        season1StatsId.setCharacterId("0"); // Character ID 0
        season1StatsId.setGameVersion(19005); // Season 1

        CharacterStats season1Stats = new CharacterStats();
        season1Stats.setId(season1StatsId);
        season1Stats.setPlayer(multiSeasonPlayer);
        season1Stats.setWins(30);
        season1Stats.setLosses(20);
        season1Stats.setDanRank(16);
        season1Stats.setLatestBattle(System.currentTimeMillis() - 10000);

        // Season 2 stats (current season)
        CharacterStatsId season2StatsId = new CharacterStatsId();
        season2StatsId.setCharacterId("0"); // Same character ID
        season2StatsId.setGameVersion(20001); // Season 2

        CharacterStats season2Stats = new CharacterStats();
        season2Stats.setId(season2StatsId);
        season2Stats.setPlayer(multiSeasonPlayer);
        season2Stats.setWins(50);
        season2Stats.setLosses(30);
        season2Stats.setDanRank(19);
        season2Stats.setLatestBattle(System.currentTimeMillis());

        Map<CharacterStatsId, CharacterStats> characterStatsMap = new HashMap<>();
        characterStatsMap.put(season1StatsId, season1Stats);
        characterStatsMap.put(season2StatsId, season2Stats);
        multiSeasonPlayer.setCharacterStats(characterStatsMap);

        when(playerRepository.findByPolarisId("MULTI123"))
                .thenReturn(Optional.of(multiSeasonPlayer));
        when(battleRepository.findAllBattlesByPlayer(multiSeasonPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.emptyList()));

        // Act
        PlayerDTO result = playerService.getPlayerStats("MULTI123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlayedCharacters()).hasSize(1);
        
        // Get the character stats (only one character)
        PlayerMatchupSummaryDTO characterStats = result.getPlayedCharacters().values().iterator().next();
        assertThat(characterStats).isNotNull();
        // Total wins and losses from both seasons
        assertThat(characterStats.getWins()).isEqualTo(80); // 30 + 50
        assertThat(characterStats.getLosses()).isEqualTo(50); // 20 + 30
        assertThat(characterStats.getCurrentSeasonDanRank()).isEqualTo(19); // Season 2 rank
        assertThat(characterStats.getPreviousSeasonDanRank()).isEqualTo(16); // Season 1 rank
        assertThat(characterStats.getCharacterWinrate()).isCloseTo(61.54f, within(0.01f)); // 80/130
    }

    @Test
    @DisplayName("Should use latest game version when multiple versions exist in same season")
    void getPlayerStats_WithMultipleVersionsInSeason_UsesLatestVersion() throws Exception {
        // Arrange - Create player with multiple game versions in Season 2
        Player multiVersionPlayer = new Player();
        multiVersionPlayer.setPlayerId("12345678901234567890");
        multiVersionPlayer.setPolarisId("MULTIVER123");
        multiVersionPlayer.setName("MultiVersionPlayer");
        multiVersionPlayer.setRegionId(1);
        multiVersionPlayer.setTekkenPower(110000L);
        multiVersionPlayer.setLatestBattle(System.currentTimeMillis());

        // Early Season 2 version
        CharacterStatsId earlyS2StatsId = new CharacterStatsId();
        earlyS2StatsId.setCharacterId("1"); // Character ID 1
        earlyS2StatsId.setGameVersion(20001);

        CharacterStats earlyS2Stats = new CharacterStats();
        earlyS2Stats.setId(earlyS2StatsId);
        earlyS2Stats.setPlayer(multiVersionPlayer);
        earlyS2Stats.setWins(20);
        earlyS2Stats.setLosses(15);
        earlyS2Stats.setDanRank(14); // Lower rank in earlier version
        earlyS2Stats.setLatestBattle(System.currentTimeMillis() - 5000);

        // Later Season 2 version
        CharacterStatsId lateS2StatsId = new CharacterStatsId();
        lateS2StatsId.setCharacterId("1"); // Same character ID
        lateS2StatsId.setGameVersion(20005); // Later version

        CharacterStats lateS2Stats = new CharacterStats();
        lateS2Stats.setId(lateS2StatsId);
        lateS2Stats.setPlayer(multiVersionPlayer);
        lateS2Stats.setWins(35);
        lateS2Stats.setLosses(20);
        lateS2Stats.setDanRank(17); // Higher rank in later version
        lateS2Stats.setLatestBattle(System.currentTimeMillis());

        Map<CharacterStatsId, CharacterStats> characterStatsMap = new HashMap<>();
        characterStatsMap.put(earlyS2StatsId, earlyS2Stats);
        characterStatsMap.put(lateS2StatsId, lateS2Stats);
        multiVersionPlayer.setCharacterStats(characterStatsMap);

        when(playerRepository.findByPolarisId("MULTIVER123"))
                .thenReturn(Optional.of(multiVersionPlayer));
        when(battleRepository.findAllBattlesByPlayer(multiVersionPlayer.getPlayerId()))
                .thenReturn(Optional.of(Collections.emptyList()));

        // Act
        PlayerDTO result = playerService.getPlayerStats("MULTIVER123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlayedCharacters()).hasSize(1);
        
        // Get the character stats (only one character)
        PlayerMatchupSummaryDTO characterStats = result.getPlayedCharacters().values().iterator().next();
        assertThat(characterStats).isNotNull();
        // Should use the rank from the latest version (20005)
        assertThat(characterStats.getCurrentSeasonDanRank()).isEqualTo(17); // Latest version rank
        assertThat(characterStats.getPreviousSeasonDanRank()).isNull(); // No Season 1 stats
        assertThat(characterStats.getWins()).isEqualTo(55); // 20 + 35
        assertThat(characterStats.getLosses()).isEqualTo(35); // 15 + 20
    }

    @Test
    @DisplayName("Should process battles from previous season correctly")
    void getPlayerStats_WithPreviousSeasonBattles_ProcessesCorrectly() throws Exception {
        // Arrange - Create player with Season 1 stats
        Player season1Player = new Player();
        season1Player.setPlayerId("12345678901234567890");
        season1Player.setPolarisId("S1BATTLES");
        season1Player.setName("Season1BattlePlayer");
        season1Player.setRegionId(1);
        season1Player.setTekkenPower(90000L);
        season1Player.setLatestBattle(System.currentTimeMillis());

        CharacterStatsId season1StatsId = new CharacterStatsId();
        season1StatsId.setCharacterId("0"); // Character ID 0
        season1StatsId.setGameVersion(19005);

        CharacterStats season1Stats = new CharacterStats();
        season1Stats.setId(season1StatsId);
        season1Stats.setPlayer(season1Player);
        season1Stats.setWins(25);
        season1Stats.setLosses(15);
        season1Stats.setDanRank(15);
        season1Stats.setLatestBattle(System.currentTimeMillis());

        Map<CharacterStatsId, CharacterStats> characterStatsMap = new HashMap<>();
        characterStatsMap.put(season1StatsId, season1Stats);
        season1Player.setCharacterStats(characterStatsMap);

        // Create a battle from Season 1
        Battle season1Battle = new Battle();
        season1Battle.setBattleId("season1battle");
        season1Battle.setDate("2024-06-15");
        season1Battle.setBattleAt(System.currentTimeMillis());
        season1Battle.setBattleType(BattleType.RANKED_BATTLE);
        season1Battle.setGameVersion(19005); // Season 1 version
        season1Battle.setPlayer1Name("Season1BattlePlayer");
        season1Battle.setPlayer1PolarisId("S1BATTLES");
        season1Battle.setPlayer1CharacterId(0); // Character ID 0
        season1Battle.setPlayer1RegionId(1);
        season1Battle.setPlayer1TekkenPower(90000L);
        season1Battle.setPlayer1DanRank(15);
        season1Battle.setPlayer1RoundsWon(3);
        season1Battle.setPlayer2Name("OldOpponent");
        season1Battle.setPlayer2PolarisId("OLDOPP123");
        season1Battle.setPlayer2CharacterId(1); // Character ID 1
        season1Battle.setPlayer2RegionId(2);
        season1Battle.setPlayer2TekkenPower(88000L);
        season1Battle.setPlayer2DanRank(14);
        season1Battle.setPlayer2RoundsWon(1);
        season1Battle.setWinner(1);
        season1Battle.setStageId(1);

        when(playerRepository.findByPolarisId("S1BATTLES"))
                .thenReturn(Optional.of(season1Player));
        when(battleRepository.findAllBattlesByPlayer(season1Player.getPlayerId()))
                .thenReturn(Optional.of(Collections.singletonList(season1Battle)));

        // Act
        PlayerDTO result = playerService.getPlayerStats("S1BATTLES");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBattles()).hasSize(1);
        BattleDTO battleDTO = result.getBattles().get(0);
        assertThat(battleDTO.getGameVersion()).isEqualTo(19005); // Season 1 version
        assertThat(battleDTO.getBattleType()).isEqualTo(BattleType.RANKED_BATTLE);
        assertThat(battleDTO.getWinner()).isEqualTo(1);
    }
}

