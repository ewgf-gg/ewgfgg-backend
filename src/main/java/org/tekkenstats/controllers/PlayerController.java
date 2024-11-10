package org.tekkenstats.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tekkenstats.dtos.CharacterStatsDTO;
import org.tekkenstats.dtos.PlayerSearchDTO;
import org.tekkenstats.dtos.PlayerStatsDTO;
import org.tekkenstats.mappers.enumsMapper;
import org.tekkenstats.models.CharacterStats;
import org.tekkenstats.models.CharacterStatsId;
import org.tekkenstats.models.Player;
import org.tekkenstats.repositories.PlayerRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/player-stats")
public class PlayerController
{
    @Autowired
    private enumsMapper enumsMapper;
    @Autowired
    private PlayerRepository playerRepository;

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    @GetMapping("/{player}")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(@PathVariable String player, HttpServletRequest request) throws InterruptedException
    {
        String clientIp = request.getRemoteAddr();
        logger.info("Received request for Player: {} from IP: {}", player, clientIp);
        return playerRepository.findByIdOrNameOrPolarisIdIgnoreCase(player)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<List<PlayerSearchDTO>> searchPlayers(@RequestParam String query) {
        if (query.length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Player> players = playerRepository.findByNameOrPolarisIdContainingIgnoreCase(query);
        List<PlayerSearchDTO> results = players.stream()
                .map(this::convertToSearchDTO)
                .limit(10) // Limit results to prevent overwhelming the UI
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    private PlayerSearchDTO convertToSearchDTO(Player player)
    {
        PlayerSearchDTO dto = new PlayerSearchDTO();
        dto.setId(player.getPlayerId());
        dto.setName(player.getName());
        dto.setTekkenId(player.getPolarisId());
        return dto;
    }



    private PlayerStatsDTO convertToDTO(Player player)
    {
        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.setPlayerId(player.getPlayerId());
        dto.setName(player.getName());
        dto.setTekkenPower(player.getTekkenPower());
        dto.setLatestBattle(player.getLatestBattle());

        Map<CharacterStatsId, CharacterStatsDTO> characterStatsMap = new TreeMap<>(
                Comparator.comparing(CharacterStatsId::getGameVersion).reversed()
                        .thenComparing(CharacterStatsId::getCharacterId)
        );

        for (Map.Entry<CharacterStatsId, CharacterStats> entry : player.getCharacterStats().entrySet())
        {
            CharacterStatsId id = entry.getKey();
            CharacterStats stats = entry.getValue();

            CharacterStatsDTO characterStatsDTO = convertToCharacterStatsDTO(id, stats);
            characterStatsMap.put(id, characterStatsDTO);
        }
        dto.setCharacterStats(characterStatsMap);
        return dto;
    }


    private CharacterStatsDTO convertToCharacterStatsDTO(CharacterStatsId id, CharacterStats stats)
    {
        CharacterStatsDTO characterStatsDTO = new CharacterStatsDTO();
        characterStatsDTO.setCharacterName(enumsMapper.getCharacterName(id.getCharacterId()));
        characterStatsDTO.setDanName(enumsMapper.getDanName(Integer.toString(stats.getDanRank())));
        characterStatsDTO.setDanRank(stats.getDanRank());
        characterStatsDTO.setWins(stats.getWins());
        characterStatsDTO.setLosses(stats.getLosses());
        return characterStatsDTO;
    }
}
