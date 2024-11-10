package org.tekkenstats.controllers;


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tekkenstats.dtos.*;
import org.tekkenstats.interfaces.CharacterWinrateProjection;
import org.tekkenstats.interfaces.PopularCharacterProjection;
import org.tekkenstats.interfaces.RankDistributionProjection;
import org.tekkenstats.mappers.enumsMapper;
import org.tekkenstats.models.TekkenStatsSummary;
import org.tekkenstats.repositories.AggregatedStatisticsRepository;
import org.tekkenstats.repositories.TekkenStatsSummaryRepository;

import java.math.BigDecimal;
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
    public ResponseEntity<CharacterWinratesDTO> getTop5Winrates() {
        logger.info("Fetching top 5 character winrates");

        try {
            // Get high rank winrates
            Map<String, Double> highRankWinrates = aggregatedStatisticsRepository
                    .findTop5CharactersByWinrateInHighRank()
                    .stream()
                    .collect(Collectors.toMap(
                            projection -> enumsMapper.getCharacterName(projection.getCharacterId()),
                            CharacterWinrateProjection::getWinratePercentage
                    ));

            Map<String, Double> mediumRankWinrates = aggregatedStatisticsRepository
                    .findTop5CharactersByWinrateInMediumRanks()
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

            CharacterWinratesDTO response = new CharacterWinratesDTO(highRankWinrates, mediumRankWinrates, lowRankWinrates);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching character winrates", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/top-popularity")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<CharacterPopularityDTO> getTop5CharacterPopularity() {
        logger.info("Fetching top 5 popular characters");

        try {
            // Get high rank popularity
            Map<String, Long> highRankPopularity = aggregatedStatisticsRepository
                    .findPopularCharactersInHighRanks()
                    .stream()
                    .collect(Collectors.toMap(
                            projection -> enumsMapper.getCharacterName(projection.getCharacterId()),
                            PopularCharacterProjection::getTotalWins
                    ));

            Map<String, Long> mediumRankPopularity = aggregatedStatisticsRepository
                    .findPopularCharactersInMediumRanks()
                    .stream()
                    .collect(Collectors.toMap(
                            projection -> enumsMapper.getCharacterName(projection.getCharacterId()),
                            PopularCharacterProjection::getTotalWins
                    ));

            // Get low rank winrates
            Map<String, Long> lowRankPopularity = aggregatedStatisticsRepository
                    .findPopularCharactersInLowRanks()
                    .stream()
                    .collect(Collectors.toMap(
                            projection -> enumsMapper.getCharacterName(projection.getCharacterId()),
                            PopularCharacterProjection::getTotalWins
                    ));

            CharacterPopularityDTO response = new CharacterPopularityDTO(highRankPopularity, mediumRankPopularity, lowRankPopularity);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching character popularity statistics", e);
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


    @GetMapping("/winrate-changes")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<Map<String, List<RankWinrateChangesDTO>>> getWinrateChanges() {
        logger.info("Fetching character winrate changes");

        try {
            List<Object[]> results = aggregatedStatisticsRepository.getWinrateChanges();

            List<RankWinrateChangesDTO> changes = results.stream()
                    .map(result -> {
                        RankWinrateChangesDTO dto = new RankWinrateChangesDTO();
                        dto.setCharacterId(enumsMapper.getCharacterName((String) result[0]));
                        dto.setRankCategory((String) result[1]);
                        dto.setChange((Double) result[2]);
                        dto.setTrend((String) result[3]);
                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, List<RankWinrateChangesDTO>> groupedChanges =
                    RankWinrateChangesDTO.groupByRankCategory(changes);

            return ResponseEntity.ok(groupedChanges);

        } catch (Exception e) {
            logger.error("Error fetching character winrate changes", e);
            return ResponseEntity.internalServerError().build();
        }
    }



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
