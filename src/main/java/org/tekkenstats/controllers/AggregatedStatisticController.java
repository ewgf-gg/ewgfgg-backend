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
import org.tekkenstats.dtos.TekkenStatsSummaryDTO;
import org.tekkenstats.dtos.rankDistributionDTO;
import org.tekkenstats.interfaces.RankDistributionProjection;
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

    @Autowired
    private TekkenStatsSummaryRepository tekkenStatsSummaryRepository;

    private static final Logger logger = LoggerFactory.getLogger(AggregatedStatisticController.class);
    @Autowired
    private AggregatedStatisticsRepository aggregatedStatisticsRepository;


    @GetMapping("/stats-summary")
    public ResponseEntity<TekkenStatsSummaryDTO> getPlayerCount(HttpServletRequest request) throws InterruptedException
    {
        logger.info("Received request for stats summary");
         return tekkenStatsSummaryRepository.getTekkenStatsSummary()
                 .map(this::convertToDTO)
                 .map(ResponseEntity::ok)
                 .orElse(ResponseEntity.notFound().build());

    }

    @GetMapping("/gameVersions")
    public ResponseEntity<List<Integer>> getGameVersions(HttpServletRequest request) throws InterruptedException
    {
        logger.info("Received request for gameVerions");
        return aggregatedStatisticsRepository.getGameVersions()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rankDistribution/{gameVersion}/{category}")
    public ResponseEntity<rankDistributionDTO> getRankDistribution(
            @PathVariable int gameVersion,
            @PathVariable String category)
    {

        logger.info("Fetching rank distribution for version: {} and category: {}", gameVersion, category);

        return Optional.of(aggregatedStatisticsRepository.getRankDistribution(gameVersion, category))
                .map(this::convertToRankDistributionDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private rankDistributionDTO convertToRankDistributionDTO(List<RankDistributionProjection> projections)
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
