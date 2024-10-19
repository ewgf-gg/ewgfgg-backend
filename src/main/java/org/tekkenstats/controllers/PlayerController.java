package org.tekkenstats.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tekkenstats.dtos.CharacterStatsDTO;
import org.tekkenstats.dtos.PlayerStatsDTO;
import org.tekkenstats.mappers.enumsMapper;
import org.tekkenstats.models.CharacterStats;
import org.tekkenstats.models.CharacterStatsId;
import org.tekkenstats.models.Player;
import org.tekkenstats.services.PlayerService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/player-stats")
public class PlayerController
{
    @Autowired
    private PlayerService playerService;
    @Autowired
    private enumsMapper enumsMapper;
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(@PathVariable String playerId, HttpServletRequest request)
    {
        String clientIp = request.getRemoteAddr();
        logger.info("Received request for Player: {} from IP: {}", playerId, clientIp);
        return playerService.getPlayerWithStats(playerId)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
