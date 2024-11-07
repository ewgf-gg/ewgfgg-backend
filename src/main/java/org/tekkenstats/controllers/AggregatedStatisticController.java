package org.tekkenstats.controllers;


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tekkenstats.dtos.CharacterPopularityDTO;
import org.tekkenstats.dtos.CharacterWinratesDTO;
import org.tekkenstats.dtos.TekkenStatsSummaryDTO;
import org.tekkenstats.dtos.rankDistributionDTO;
import org.tekkenstats.interfaces.CharacterWinrateProjection;
import org.tekkenstats.interfaces.RankDistributionProjection;
import org.tekkenstats.mappers.enumsMapper;
import org.tekkenstats.models.TekkenStatsSummary;
import org.tekkenstats.repositories.AggregatedStatisticsRepository;
import org.tekkenstats.repositories.TekkenStatsSummaryRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
public class AggregatedStatisticController
{

    private final TekkenStatsSummaryRepository tekkenStatsSummaryRepository;
    private final AggregatedStatisticsRepository aggregatedStatisticsRepository;
    private final enumsMapper enumsMapper;

    private static final Logger logger = LoggerFactory.getLogger(AggregatedStatisticController.class);

    public AggregatedStatisticController(
            TekkenStatsSummaryRepository tekkenStatsSummaryRepository,
            AggregatedStatisticsRepository aggregatedStatisticsRepository,
            enumsMapper enumsMapper)
    {
        this.tekkenStatsSummaryRepository = tekkenStatsSummaryRepository;
        this.aggregatedStatisticsRepository = aggregatedStatisticsRepository;
        this.enumsMapper = enumsMapper;
    }

    @GetMapping("/stats-summary")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<TekkenStatsSummaryDTO> getPlayerCount(HttpServletRequest request) throws InterruptedException
    {
        logger.info("Received request for stats summary");
         return tekkenStatsSummaryRepository.getTekkenStatsSummary()
                 .map(this::convertToDTO)
                 .map(ResponseEntity::ok)
                 .orElse(ResponseEntity.notFound().build());

    }

    @GetMapping("/top-winrates")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<CharacterWinratesDTO> getTopWinrates() {
        logger.info("Fetching top 5 character winrates for both high and low ranks");

        try {
            // Get high rank winrates
            Map<String, Double> highRankWinrates = aggregatedStatisticsRepository
                    .findTop5CharactersByWinrateInStandard()
                    .stream()
                    .collect(Collectors.toMap(
                            projection -> enumsMapper.getCharacterName(projection.getCharacterId()),
                            CharacterWinrateProjection::getWinratePercentage
                    ));

            // Get low rank winrates
            Map<String, Double> lowRankWinrates = aggregatedStatisticsRepository
                    .findTop5CharactersByWinrateInLowRanks()
                    .stream()
                    .collect(Collectors.toMap(
                            projection -> enumsMapper.getCharacterName(projection.getCharacterId()),
                            CharacterWinrateProjection::getWinratePercentage
                    ));

            CharacterWinratesDTO response = new CharacterWinratesDTO(highRankWinrates, lowRankWinrates);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching character winrates", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/gameVersions")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<List<Integer>> getGameVersions(HttpServletRequest request) throws InterruptedException
    {
        logger.info("Received request for gameVerions");
        return aggregatedStatisticsRepository.getGameVersions()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rankDistribution/{gameVersion}/{category}")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<rankDistributionDTO> getRankDistribution(
            @PathVariable int gameVersion,
            @PathVariable String category)
    {

        logger.info("Fetching rank distribution for version: {} and category: {}", gameVersion, category);

        return Optional.of(aggregatedStatisticsRepository.getRankDistribution(gameVersion, category))
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    public ResponseEntity<CharacterPopularityDTO> getCharacterPopularityInHighRanks()
    {
        logger.info("Fetching character popularity for high rank");
    }

    private CharacterPopularityDTO convertToDTO()

    private rankDistributionDTO convertToDTO(List<RankDistributionProjection> projections)
    {
        rankDistributionDTO dto = new rankDistributionDTO();
        Map<Integer, Double> distribution = projections.stream()
                .collect(Collectors.toMap(
                        RankDistributionProjection::getRank,
                        RankDistributionProjection::getPercentage
                ));
        dto.setRankDistribution(distribution);
        return dto;
    }


    private TekkenStatsSummaryDTO convertToDTO(TekkenStatsSummary tekkenStatsSummary)
    {
        TekkenStatsSummaryDTO dto = new TekkenStatsSummaryDTO();
        dto.setTotalPlayers(tekkenStatsSummary.getTotalPlayers());
        dto.setTotalReplays(tekkenStatsSummary.getTotalReplays());
        return dto;
    }
}
