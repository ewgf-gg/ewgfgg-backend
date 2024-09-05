package org.tekkenstats.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tekkenstats.Battle;
import org.tekkenstats.interfaces.BattleRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class APIService {

    @Autowired
    private ReplayService replayService;
    @Autowired
    private BattleRepository battleRepository;

    private static final Logger logger = LogManager.getLogger(APIService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int TIME_STEP = 700;

    private String API_URL = "https://wank.wavu.wiki/api/replays";
    private static final ZoneId zoneId = ZoneId.systemDefault();

    long unixTimestamp = 1725525850L;

    @Scheduled(fixedRate = 1000)
    public void fetchAndProcessReplays() {

        String readableDate = Instant.ofEpochSecond(unixTimestamp)
                .atZone(zoneId)
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));

        logger.info("Sending query to API endpoint for battle time before: {}, Unix: {}", readableDate, unixTimestamp);
        try {
            // Fetch JSON from the external API
            String jsonResponse = restTemplate.getForObject(API_URL + "?before=" + unixTimestamp, String.class);

            // Convert JSON to Battle object
            List<Battle> battles = objectMapper.readValue(jsonResponse, new TypeReference<List<Battle>>() {});
            logger.info("Received {} battles from API", battles.size());
            // Start the timer for bulk operation
            long startTime = System.currentTimeMillis();

            // Bulk process all battles at once
            replayService.processBattles(battles);  // Assuming processBattles is a new method to handle bulk operations

            // End the timer for bulk operation
            long endTime = System.currentTimeMillis();

            // Log the total time taken for the bulk write operation
            logger.info("Bulk write process of {} battles took {} ms", battles.size(), (endTime - startTime));

            unixTimestamp -= TIME_STEP;
        } catch (Exception e) {
            // Handle exceptions (e.g., logging)
            logger.error(e.getMessage());
        }
    }
}
