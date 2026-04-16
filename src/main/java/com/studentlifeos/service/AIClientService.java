package com.studentlifeos.service;

import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.dto.VoiceMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Production-grade AI Client Service with high-latency tolerance for Hugging Face Spaces.
 * Implements intent-aware fallbacks and 120s diagnostic timeouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIClientService {

    private final WebClient aiBrainWebClient;

    @Value("${ai.brain.base-url}")
    private String baseUrl;

    @Value("${ai.brain.read-timeout-ms:120000}") // 2 mins for heavy coding analysis
    private long timeoutMs;

    @Value("${ai.brain.max-retries:3}")
    private int maxRetries;

    @Value("${ai.brain.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${ai.brain.circuit-breaker.recovery-timeout-ms:60000}")
    private long recoveryTimeoutMs;

    // Circuit breaker state
    private volatile boolean circuitOpen = false;
    private volatile long lastFailureTime = 0;
    private volatile int consecutiveFailures = 0;

    /**
     * Synchronous AI request optimized for Dashboard and Coding modules.
     */
    public AiBrainResponse generate(@NonNull AiBrainRequest request) {
        if (isCircuitOpen()) {
            log.warn("Neural Link is OPEN (Suspended). Delivering emergency protocol for intent: {}", request.getIntent());
            return buildFallbackResponse(request);
        }

        try {
            log.info("Neural Link: Transmitting request to Brain (Intent: {})", request.getIntent());

            AiBrainResponse response = aiBrainWebClient.post()
                    .uri(baseUrl + "/ai/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiBrainResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    // 🛡️ V3 SAFETY: Disable retries entirely for chat to prevent key depletion
                    // One user click = Exactly 1 API request.
                    .retryWhen(Retry.max(0))
                    .onErrorResume(Exception.class, ex -> {
                        log.error("Neural Link Request Failed: {}", ex.getMessage());
                        recordFailure();
                        return Mono.empty();
                    })
                    .doOnSuccess(res -> {
                        if (res != null && res.getReplyText() != null) recordSuccess();
                        else recordFailure();
                    })
                    .block();

            if (response == null || response.getReplyText() == null) {
                return buildFallbackResponse(request);
            }

            return response;

        } catch (Exception e) {
            log.error("CRITICAL: Neural link synchronization interrupted", e);
            recordFailure();
            return buildFallbackResponse(request);
        }
    }

    /**
     * Asynchronous AI request for non-blocking UI background tasks.
     */
    public Mono<AiBrainResponse> generateAsync(@NonNull AiBrainRequest request) {
        if (isCircuitOpen()) return Mono.just(buildFallbackResponse(request));

        return aiBrainWebClient.post()
                .uri(baseUrl + "/ai/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiBrainResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(res -> recordSuccess())
                .doOnError(e -> recordFailure())
                .onErrorReturn(buildFallbackResponse(request));
    }

    /**
     * Streaming AI response for real-time terminal feedback.
     */
    public Mono<String> generateStream(@NonNull AiBrainRequest request) {
        if (isCircuitOpen()) return Mono.just("Neural link unstable. Manual override required.");

        return aiBrainWebClient.post()
                .uri(baseUrl + "/ai/generate/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(res -> recordSuccess())
                .doOnError(e -> recordFailure())
                .onErrorReturn("Connection to core lost during transmission.");
    }

    public boolean isHealthy() {
        try {
            return aiBrainWebClient.get().uri(baseUrl + "/health").retrieve().bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5)).map("ok"::equals).onErrorReturn(false).block();
        } catch (Exception e) { return false; }
    }

    // ==================== Circuit Breaker Logic ====================

    private boolean isCircuitOpen() {
        if (!circuitOpen) return false;
        if (System.currentTimeMillis() - lastFailureTime > recoveryTimeoutMs) {
            log.info("Neural Link: Attempting recovery (Circuit transitioning to HALF-OPEN)");
            circuitOpen = false;
            return false;
        }
        return true;
    }

    private void recordSuccess() {
        consecutiveFailures = 0;
        circuitOpen = false;
    }

    private void recordFailure() {
        consecutiveFailures++;
        lastFailureTime = System.currentTimeMillis();
        if (consecutiveFailures >= failureThreshold) {
            circuitOpen = true;
            log.error("Neural Link SUSPENDED: Circuit OPEN after {} failures", consecutiveFailures);
        }
    }

    // ==================== Intent-Aware Fallbacks ====================

    private AiBrainResponse buildFallbackResponse(AiBrainRequest request) {
        String msg = switch (request.getIntent()) {
            case "CODING_ASSIST" -> "Hugging Face core is processing heavy logic. If this continues, ensure Space is 'Running'.";
            case "WELLNESS_CHECKIN" -> "Neural link is weak, but I'm here. Take a slow breath while I reconnect.";
            case "STUDY_PLANNING" -> "I can't access your timeline right now. Please check your network sync.";
            default -> "Mavis is momentarily offline. Synchronizing with local buffers...";
        };

        return AiBrainResponse.builder()
                .replyText(msg)
                .emotion(Emotion.SERIOUS)
                .mascotAction(MascotAction.THINKING)
                .voiceMeta(VoiceMeta.serious())
                .build();
    }

    /**
     * Get circuit breaker status for monitoring
     */
    public CircuitBreakerStatus getCircuitBreakerStatus() {
        return new CircuitBreakerStatus(
                circuitOpen ? "OPEN" : "CLOSED",
                consecutiveFailures,
                failureThreshold,
                lastFailureTime
        );
    }

    public record CircuitBreakerStatus(
            String state,
            int consecutiveFailures,
            int failureThreshold,
            long lastFailureTime
    ) {}
}
