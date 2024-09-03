package org.tekkenstats.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tekkenstats.Battle;

@Service
public class APIService {

    @Autowired
    private ReplayService replayService;

    private static final Logger logger = LogManager.getLogger(APIService.class);


    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String API_URL = "https://wank.wavu.wiki/api/replays"; // Example URL

    @Scheduled(fixedRate = 10000)
    public void fetchAndProcessReplays() {

        logger.info("Sending query to API endpoint");
        try {
            // Fetch JSON from the external API
            String jsonResponse = restTemplate.getForObject(API_URL, String.class);

            // Convert JSON to Battle object
            Battle battle = objectMapper.readValue(jsonResponse, Battle.class);

            // Process the battle data internally
            replayService.saveBattleData(battle);

        } catch (Exception e) {
            // Handle exceptions (e.g., logging)
            logger.error(e.getMessage());
        }
    }
}
