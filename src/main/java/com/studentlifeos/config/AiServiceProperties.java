package com.studentlifeos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AI service integration.
 * Provides type-safe configuration binding from application.yml.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.brain")
public class AiServiceProperties {
    
    /**
     * Base URL for the AI service
     */
    private String baseUrl = "https://mrbaddy-student-life-os-ai-docker.hf.space";
    
    /**
     * Connection timeout in milliseconds
     */
    private int connectTimeoutMs = 5000;
    
    /**
     * Read timeout in milliseconds
     */
    private int readTimeoutMs = 10000;
    
    /**
     * Maximum number of retry attempts
     */
    private int maxRetries = 3;
    
    /**
     * Overall request timeout in milliseconds
     */
    private long timeoutMs = 30000;
    
    /**
     * Circuit breaker configuration
     */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    
    @Data
    public static class CircuitBreaker {
        /**
         * Number of consecutive failures before opening circuit
         */
        private int failureThreshold = 5;
        
        /**
         * Time in milliseconds to wait before attempting recovery
         */
        private long recoveryTimeoutMs = 60000;
    }
}
