package org.tekkenstats.configuration;

import lombok.Data;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Component
@Data
public class BackpressureManager {

    @Value("${rabbitmq.management.api.url}")
    private String rabbitMqApiUrl;

    @Value("${rabbitmq.management.username}")
    private String rabbitMqApiUsername;

    @Value("${rabbitmq.management.password}")
    private String rabbitMqApiPassword;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${backpressure.threshold}")
    private int backpressureThreshold;

    @Value("${backpressure.slowdown.factor}")
    private int slowdownFactor;

    private volatile boolean backpressureActive = false;

    private static final Logger logger = LogManager.getLogger(BackpressureManager.class);

    private RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 10000) // Check every 5 seconds
    public void monitorQueueDepth() {
        try {
            int messageCount = getQueueMessageCount();
            if (messageCount > backpressureThreshold && !backpressureActive) {
                logger.warn("BOTTLENECK DETECTED, ENABLING BACKPRESSURE");
                activateBackpressure();
            } else if (messageCount <= 2 && backpressureActive) {
                logger.info("Backlog emptied! Resuming normal operation");
                deactivateBackpressure();
            }
        } catch (Exception e) {
            logger.error("Failed to fetch queue depth from RabbitMQ API", e);
        }
    }

    private int getQueueMessageCount() throws URISyntaxException {
        String url = UriComponentsBuilder.fromHttpUrl(rabbitMqApiUrl)
                    .pathSegment("queues", "%2F", queueName)
                    .build()
                    .toUriString(); // This ensures %2F remains encoded

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(rabbitMqApiUsername, rabbitMqApiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(new URI(url), HttpMethod.GET, entity, Map.class);
        Map<String, Object> queueInfo = response.getBody();
        return (int) queueInfo.get("messages");
        }


    private void activateBackpressure() {
        backpressureActive = true;
        // Slow down consumers or other logic for backpressure
    }

    private void deactivateBackpressure() {
        backpressureActive = false;
        // Resume normal operation
    }
}
