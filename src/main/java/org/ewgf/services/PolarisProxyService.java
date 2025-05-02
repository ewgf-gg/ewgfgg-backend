package org.ewgf.services;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class PolarisProxyService {

    private final RestTemplate restTemplate;
        private final String BASE_PATH;

    public PolarisProxyService(RestTemplateBuilder restTemplateBuilder,
                               @Value("${polaris.api.baseUrl}") String POLARIS_BASE_URL,
                               @Value("${polaris.api.basePath}") String BASE_PATH,
                               @Value("${polaris.api.timeout:5000}") int timeoutMillis) {
        this.restTemplate = restTemplateBuilder
                .rootUri(POLARIS_BASE_URL)
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .readTimeout(Duration.ofMillis(timeoutMillis))
                .build();
        this.BASE_PATH = BASE_PATH;
    }

    public StatPentagonResponse fetchStatPentagonFromProxy(Map<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        String url = BASE_PATH + GET_PROFILE;
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(
                params,
                headers
        );

        try {
            ResponseEntity<StatPentagonResponse[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    StatPentagonResponse[].class
            );

            StatPentagonResponse[] responseArray = response.getBody();
            return responseArray != null && responseArray.length > 0 ? responseArray[0] : null;
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error(ERROR_CODE_429_MESSAGE);
            throw e;
        }
        catch (Exception e) {
            log.error("Error fetching stat pentagon from proxy server: {}", e.getMessage());
            throw e;
        }
    }
}