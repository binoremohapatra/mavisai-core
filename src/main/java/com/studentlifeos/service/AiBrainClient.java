package com.studentlifeos.service;

import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiBrainClient {

    private final RestTemplate restTemplate;

    @Value("${ai.brain.base-url:http://localhost:9001}")
    private String baseUrl;

    public AiBrainResponse generate(AiBrainRequest request) {
        String url = baseUrl + "/ai/generate";
        try {
            return restTemplate.postForObject(url, request, AiBrainResponse.class);
        } catch (RestClientException ex) {
            log.warn("AI Brain call failed: {}", ex.getMessage());
            return null;
        }
    }
}
