package com.studentlifeos.service;

import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiBrainWebClient {

    /** Injects WebClient from WebClientConfig */
    private final WebClient aiBrainWebClient;

    @Value("${ai.brain.base-url:http://localhost:9001}")
    private String baseUrl;

    public AiBrainResponse generate(@NonNull AiBrainRequest request) {
        log.debug("Sending request to AI Brain: {}", request.getIntent());

        try {
            return aiBrainWebClient.post()
                    .uri(baseUrl + "/ai/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiBrainResponse.class)
                    .timeout(Duration.ofMillis(30000))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .doBeforeRetry(retry -> log.warn("Retrying AI request (attempt {})", 
                                    retry.totalRetries() + 1)))
                    .onErrorResume(WebClientRequestException.class, ex -> {
                        log.error("AI Brain connection error: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("AI Brain request failed: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to call AI Brain service", e);
            return null;
        }
    }

    public Mono<AiBrainResponse> generateAsync(@NonNull AiBrainRequest request) {
        return aiBrainWebClient
                .post()
                .uri(baseUrl + "/ai/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiBrainResponse.class)
                .timeout(Duration.ofMillis(30000))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .doBeforeRetry(retry -> 
                                log.warn("Retrying AI Brain (attempt {})", 
                                        retry.totalRetries() + 1)))
                .doOnError(WebClientRequestException.class, 
                        e -> log.error("AI Brain connection failed", e))
                .doOnError(e -> 
                        log.error("AI Brain request failed", e))
                .onErrorResume(e -> Mono.empty());
    }

    /** Streaming response (SSE / chunked text) */
    public Mono<String> generateStream(@NonNull AiBrainRequest request) {
        return aiBrainWebClient
                .post()
                .uri(baseUrl + "/ai/generate/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .timeout(Duration.ofMillis(60000))
                .onErrorResume(e -> {
                    log.warn("AI Brain stream failed", e);
                    return Mono.just("");
                });
    }
}
