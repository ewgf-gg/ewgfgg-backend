package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;
import org.ewgf.response.CombinedLeaderboardResponse;
import org.ewgf.response.StatPentagonResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

import static org.ewgf.utils.Constants.ERROR_CODE_429_MESSAGE;
import static org.ewgf.utils.Constants.GET_PROFILE;
import static org.ewgf.utils.Constants.GET_LEADERBOARD_DATA;

@Slf4j
@Service
public class PolarisProxyService {

    private final RestTemplate restTemplate;
    private final String BASE_PATH;

    public PolarisProxyService(RestTemplateBuilder restTemplateBuilder,
                               @Value("${polaris.api.baseUrl}") String polarisBaseUrl,
                               @Value("${polaris.api.basePath}") String BASE_PATH,
                               @Value("${polaris.api.timeout:5000}") int timeoutMillis) {
        this.restTemplate = restTemplateBuilder
                .rootUri(polarisBaseUrl)
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .readTimeout(Duration.ofMillis(timeoutMillis))
                .build();
        this.BASE_PATH = BASE_PATH;
    }

    private <T> T executeProxyRequest(String url, Map<String, String> params, Class<T[]> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<T[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    responseType
            );

            T[] responseArray = response.getBody();
            return responseArray != null && responseArray.length > 0 ? responseArray[0] : null;
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error(ERROR_CODE_429_MESSAGE);
            throw e;
        } catch (Exception e) {
            log.error("Error executing request to proxy server: {}", e.getMessage());
            throw e;
        }
    }

    public StatPentagonResponse fetchStatPentagonFromProxy(Map<String, String> params) {
        return executeProxyRequest(BASE_PATH + GET_PROFILE, params, StatPentagonResponse[].class);
    }

    public CombinedLeaderboardResponse fetchLeaderboardFromProxy(Map<String, String> params) {
        return executeProxyRequest(BASE_PATH + GET_LEADERBOARD_DATA, params, CombinedLeaderboardResponse[].class);
    }
}