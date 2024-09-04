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

    private String API_URL = "https://wank.wavu.wiki/api/replays";// Example URL

    long before = 1725412760;


    @Scheduled(fixedRate = 1000)
    public void fetchAndProcessReplays() {

        logger.info("Sending query to API endpoint");
        try {
            // Fetch JSON from the external API
            String jsonResponse = restTemplate.getForObject(API_URL + "?before=" + before, String.class);

            // Convert JSON to Battle object
            List<Battle> battles = objectMapper.readValue(jsonResponse, new TypeReference<List<Battle>>() {});

            // Loop through each array and process each Battle
            for (Battle battle : battles)
            {
                // Process the battle data internally
                replayService.saveBattleData(battle);
            }
            before -= TIME_STEP;
        } catch (Exception e) {
            // Handle exceptions (e.g., logging)
            logger.error(e.getMessage());
        }
    }
}
