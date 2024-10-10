package org.tekkenstats.services;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.util.List;

@Service
@Data
public class BattleBloomFilterService
{

    private static final Logger logger = LoggerFactory.getLogger(BattleBloomFilterService.class);
    private static final int BLOOM_FILTER_SIZE = 1000000;
    private static final double FALSE_POSITIVE_PROBABILITY = 0.01;

    private volatile BloomFilter<String> battleIdBloomFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Constructor injection
    public BattleBloomFilterService(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
        refreshBattleIdBloomFilter();
    }

    @Scheduled(fixedRate = 300000) //5 minutes
    public void refreshBattleIdBloomFilter()
    {
        logger.info("Refreshing Battle ID Bloom Filter...");
        List<String> battleIds = jdbcTemplate.queryForList(
                "SELECT battle_id FROM battles ORDER BY battle_at DESC LIMIT 1000000", String.class);

        BloomFilter<String> newBloomFilter = BloomFilter.create(
                Funnels.unencodedCharsFunnel(),
                BLOOM_FILTER_SIZE,
                FALSE_POSITIVE_PROBABILITY
        );

        for (String battleId : battleIds)
        {
            newBloomFilter.put(battleId);
        }

        battleIdBloomFilter = newBloomFilter;
        logger.info("Bloom Filter refreshed with {} battle IDs.", battleIds.size());
    }
}
