package com.studentlifeos.controller;

import com.studentlifeos.config.AiServiceProperties;
import com.studentlifeos.service.AIClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check and monitoring controller for AI service integration.
 * Provides insights into AI service status and circuit breaker state.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiHealthController {

    private final AIClientService aiClientService;
    private final AiServiceProperties aiServiceProperties;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        boolean isHealthy = aiClientService.isHealthy();
        AIClientService.CircuitBreakerStatus circuitStatus = aiClientService.getCircuitBreakerStatus();
        
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("aiServiceUrl", aiServiceProperties.getBaseUrl());
        health.put("circuitBreaker", Map.of(
                "state", circuitStatus.state(),
                "consecutiveFailures", circuitStatus.consecutiveFailures(),
                "failureThreshold", circuitStatus.failureThreshold(),
                "lastFailureTime", circuitStatus.lastFailureTime()
        ));
        health.put("configuration", Map.of(
                "timeoutMs", aiServiceProperties.getTimeoutMs(),
                "maxRetries", aiServiceProperties.getMaxRetries(),
                "connectTimeoutMs", aiServiceProperties.getConnectTimeoutMs(),
                "readTimeoutMs", aiServiceProperties.getReadTimeoutMs()
        ));
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/circuit-breaker")
    public ResponseEntity<AIClientService.CircuitBreakerStatus> getCircuitBreakerStatus() {
        return ResponseEntity.ok(aiClientService.getCircuitBreakerStatus());
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testAiService() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            boolean isHealthy = aiClientService.isHealthy();
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("success", isHealthy);
            result.put("responseTimeMs", responseTime);
            result.put("message", isHealthy ? "AI service is responding normally" : "AI service is not responding");
            
        } catch (Exception e) {
            log.error("AI service test failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "AI service test failed");
        }
        
        return ResponseEntity.ok(result);
    }
}
