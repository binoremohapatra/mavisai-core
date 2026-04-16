package com.studentlifeos.service;

import com.studentlifeos.config.AiServiceProperties;
import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonAIService {

    private final AiServiceProperties aiProps;
    private final RestTemplate restTemplate;

    /**
     * Call the Python Brain to generate text/emotions (LLM)
     */
    public AiBrainResponse generateThought(AiBrainRequest request) {
        String url = aiProps.getBaseUrl() + "/ai/generate";
        log.debug("Calling Brain: {}", url);

        try {
            return restTemplate.postForObject(url, request, AiBrainResponse.class);
        } catch (Exception e) {
            log.error("Brain Connection Failed: {}", e.getMessage());
            return AiBrainResponse.builder()
                    .replyText("I am having trouble connecting to my brain.")
                    .build();
        }
    }

    /**
     * Call the Python Mouth to generate audio (Edge-TTS)
     */
    public byte[] generateAudio(String text) {
        String url = aiProps.getBaseUrl() + "/ai/speak";
        log.debug("Requesting Audio: {}", url);

        // Prepare JSON Payload
        Map<String, String> payload = new HashMap<>();
        payload.put("text", text);
        payload.put("voice", "en-US-EmmaNeural"); // Using the 'Human' voice
        payload.put("pitch", "-2Hz");
        payload.put("rate", "+0%");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        try {
            // We expect a byte array (MP3 data) back
            return restTemplate.postForObject(url, entity, byte[].class);
        } catch (Exception e) {
            log.error("TTS Generation Failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean isHealthy() {
        try {
            String url = aiProps.getBaseUrl() + "/health";
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
