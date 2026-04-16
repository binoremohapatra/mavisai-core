package com.studentlifeos.service;

import com.studentlifeos.dto.*;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestratorService {

    private final AIClientService aiClientService;
    private final PythonAIService pythonAIService;

    // 🧠 CACHE ENGINE
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_SECONDS = 3600; // 1 Hour TTL
    private static final int MAX_CACHE_SIZE = 1000;

    public ApiResponse<?> handleChat(AIChatRequest request) {
        String userMessage = (request.getMessage() != null) ? request.getMessage().trim() : "";
        String cacheKey = userMessage.toLowerCase();

        log.info("Processing chat request: {}", userMessage);

        // 1. ⚡ QUICK BYPASS: Common Greetings (No API Hit)
        if (isCommonGreeting(cacheKey)) {
            return buildStaticGreeting();
        }

        // 2. 🔍 CACHE LOOKUP: Check for existing, non-expired response
        if (responseCache.containsKey(cacheKey)) {
            CachedResponse cached = responseCache.get(cacheKey);
            if (Instant.now().getEpochSecond() < cached.getExpiryTime()) {
                log.info("🚀 Cache Hit! Reusing response for: {}", cacheKey);
                return buildFinalResponse(cached.getResponse(), generateAudio(cached.getResponse().getReplyText()));
            } else {
                responseCache.remove(cacheKey); // Expired
            }
        }

        // 3. CALL AI BRAIN (Groq/Gemini)
        Map<String, Object> facts = new HashMap<>();
        facts.put("userId", request.getUserId());

        AiBrainRequest brainRequest = AiBrainRequest.builder()
                .intent("CHAT")
                .userMessage(userMessage)
                .facts(facts)
                .desiredTone("friendly")
                .language("en")
                .build();

        AiBrainResponse response = aiClientService.generate(brainRequest);

        // Fallback for empty AI responses
        if (response == null || response.getReplyText() == null) {
            response = createFallbackResponse();
        }

        // 4. ✅ DETERMINE BODY & FACE (Based on logic)
        MascotAction action = determineAction(userMessage, response.getReplyText());
        Emotion emotion = determineEmotion(action, userMessage, response.getReplyText());

        response.setMascotAction(action);
        response.setEmotion(emotion);

        // 5. 💾 UPDATE CACHE
        saveToCache(cacheKey, response);

        // 6. GENERATE AUDIO & RETURN
        return buildFinalResponse(response, generateAudio(response.getReplyText()));
    }

    // =================================================================
    // 🛡️ CACHE HELPERS
    // =================================================================

    private void saveToCache(String key, AiBrainResponse response) {
        if (responseCache.size() >= MAX_CACHE_SIZE) {
            responseCache.clear(); // Emergency cleanup
        }
        long expiry = Instant.now().getEpochSecond() + CACHE_TTL_SECONDS;
        responseCache.put(key, new CachedResponse(response, expiry));
    }

    private boolean isCommonGreeting(String msg) {
        return msg.matches("^(hi|hello|hey|namaste|hola|hlo|helo)$");
    }

    private ApiResponse<?> buildStaticGreeting() {
        return ApiResponse.builder()
                .replyText("Hello! Mavis neural link active. How can I assist your studies today?")
                .emotion(Emotion.HAPPY)
                .mascotAction(MascotAction.WAVE)
                .build();
    }

    private String generateAudio(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            byte[] audioData = pythonAIService.generateAudio(text);
            return (audioData != null) ? Base64.getEncoder().encodeToString(audioData) : null;
        } catch (Exception e) {
            log.error("Audio generation failed: {}", e.getMessage());
            return null;
        }
    }

    private ApiResponse<?> buildFinalResponse(AiBrainResponse res, String audioBase64) {
        return ApiResponse.builder()
                .replyText(res.getReplyText())
                .emotion(res.getEmotion())
                .mascotAction(res.getMascotAction())
                .audioBase64(audioBase64)
                .build();
    }

    // =================================================================
    // 🕺 ANIMATION & EMOTION LOGIC
    // =================================================================

    public MascotAction determineAction(String userMessage, String aiReply) {
        String input = (userMessage != null) ? userMessage.toLowerCase() : "";
        String reply = (aiReply != null) ? aiReply.toLowerCase() : "";

        if (matches(input, "hi", "hello", "hey", "bye", "wave")) return MascotAction.WAVE;
        if (matches(input, "won", "win", "success", "congrats") || matches(reply, "congratulations", "proud")) return MascotAction.VICTORY;
        if (matches(input, "thanks", "thank", "grateful", "appreciate")) return MascotAction.THANKFUL;
        if (matches(input, "stupid", "idiot", "hate", "angry")) return MascotAction.ANGRY;
        if (matches(input, "sorry", "sad", "fail", "hurt") || matches(reply, "apologize", "unfortunately")) return MascotAction.SAD;
        if (matches(input, "think", "what", "why", "how", "hmm")) return MascotAction.THINKING;

        return MascotAction.IDLE;
    }

    public Emotion determineEmotion(MascotAction action, String userMessage, String aiReply) {
        String input = (userMessage != null) ? userMessage.toLowerCase() : "";

        switch (action) {
            case WAVE: return Emotion.FRIENDLY;
            case VICTORY: return Emotion.CELEBRATORY;
            case ANGRY: return Emotion.ANGRY;
            case SAD: return Emotion.SAD;
            case THANKFUL: return Emotion.SUPPORTIVE;
        }

        if (matches(input, "wow", "omg", "shock")) return Emotion.SURPRISED;
        if (matches(input, "excited", "can't wait")) return Emotion.EXCITED;
        if (matches(input, "what", "huh", "don't understand")) return Emotion.CONFUSED;
        if (matches(input, "relax", "calm", "peace")) return Emotion.CALM;
        if (matches(input, "urgent", "important", "exam")) return Emotion.SERIOUS;
        if (matches(input, "good", "nice", "happy")) return Emotion.HAPPY;

        return Emotion.NEUTRAL;
    }

    private boolean matches(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private AiBrainResponse createFallbackResponse() {
        return AiBrainResponse.builder()
                .replyText("Neural link established. How can I assist?")
                .emotion(Emotion.FRIENDLY)
                .mascotAction(MascotAction.IDLE)
                .build();
    }

    // 📦 Internal Helper for TTL Cache
    @Getter
    @AllArgsConstructor
    private static class CachedResponse {
        private final AiBrainResponse response;
        private final long expiryTime;
    }
}