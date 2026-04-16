package com.studentlifeos.service;

import com.studentlifeos.domain.WellnessCheckin;
import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import com.studentlifeos.dto.WellnessCheckinRequest;
import com.studentlifeos.dto.WellnessSuggestionPayload;
import com.studentlifeos.repository.WellnessCheckinRepository;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.enums.Emotion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Production-grade Wellness Service with AI-powered wellness guidance.
 * Provides intelligent wellness suggestions and emotional support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WellnessService {

    private final WellnessCheckinRepository checkinRepository;
    private final AIClientService aiClientService;

    /**
     * AI-powered wellness check-in with personalized suggestions
     */
    public WellnessSuggestionPayload checkin(WellnessCheckinRequest request) {
        log.info("Processing AI-powered wellness check-in for user: {}", request.getUserId());

        // Save the check-in first
        WellnessCheckin entity = new WellnessCheckin();
        entity.setUserId(request.getUserId());
        entity.setMoodNote(request.getMoodNote());
        entity.setCreatedAt(LocalDateTime.now());
        checkinRepository.save(entity);

        try {
            AiBrainRequest aiRequest = buildWellnessAiRequest(request);
            AiBrainResponse aiResponse = aiClientService.generate(aiRequest);

            if (aiResponse == null || !StringUtils.hasText(aiResponse.getReplyText())) {
                log.warn("AI service returned empty response for wellness check-in");
                return buildFallbackWellnessResponse(request);
            }

            return parseAiResponseToWellness(aiResponse, request);

        } catch (Exception e) {
            log.error("Wellness check-in failed for user: {}", request.getUserId(), e);
            return buildFallbackWellnessResponse(request);
        }
    }

    /**
     * Provides AI-powered wellness tips based on current mood
     */
    public WellnessSuggestionPayload getWellnessTips(Long userId, String currentMood) {
        log.info("Getting AI wellness tips for user: {}, mood: {}", userId, currentMood);

        try {
            Map<String, Object> facts = new HashMap<>();
            facts.put("userId", userId);
            facts.put("currentMood", currentMood);
            facts.put("requestType", "WELLNESS_TIPS");

            AiBrainRequest aiRequest = AiBrainRequest.builder()
                    .intent("WELLNESS_CHECKIN")
                    .facts(facts)
                    .userMessage(String.format("Provide wellness suggestions for someone feeling %s.", currentMood))
                    .desiredTone("supportive")
                    .language("en")
                    .build();

            AiBrainResponse aiResponse = aiClientService.generate(aiRequest);
            
            if (aiResponse == null || !StringUtils.hasText(aiResponse.getReplyText())) {
                return buildFallbackWellnessTips(currentMood);
            }

            return parseAiResponseToWellness(aiResponse, null);

        } catch (Exception e) {
            log.error("Wellness tips failed for user: {}", userId, e);
            return buildFallbackWellnessTips(currentMood);
        }
    }

    /**
     * AI-powered stress management guidance
     */
    public WellnessSuggestionPayload getStressManagement(Long userId, String stressLevel) {
        log.info("Getting AI stress management for user: {}, stress level: {}", userId, stressLevel);

        try {
            Map<String, Object> facts = new HashMap<>();
            facts.put("userId", userId);
            facts.put("stressLevel", stressLevel);
            facts.put("requestType", "STRESS_MANAGEMENT");

            AiBrainRequest aiRequest = AiBrainRequest.builder()
                    .intent("WELLNESS_CHECKIN")
                    .facts(facts)
                    .userMessage(String.format("Provide stress management techniques for %s stress level.", stressLevel))
                    .desiredTone("calm")
                    .language("en")
                    .build();

            AiBrainResponse aiResponse = aiClientService.generate(aiRequest);
            
            if (aiResponse == null || !StringUtils.hasText(aiResponse.getReplyText())) {
                return buildFallbackStressManagement(stressLevel);
            }

            return parseAiResponseToWellness(aiResponse, null);

        } catch (Exception e) {
            log.error("Stress management failed for user: {}", userId, e);
            return buildFallbackStressManagement(stressLevel);
        }
    }

    // ==================== Private Helper Methods ====================

    private AiBrainRequest buildWellnessAiRequest(WellnessCheckinRequest request) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("userId", request.getUserId());
        facts.put("moodNote", request.getMoodNote());
        facts.put("requestType", "WELLNESS_CHECKIN");

        return AiBrainRequest.builder()
                .intent("WELLNESS_CHECKIN")
                .facts(facts)
                .userMessage(String.format("Provide supportive wellness guidance based on this check-in: %s", 
                        request.getMoodNote()))
                .desiredTone("supportive")
                .language("en")
                .build();
    }

    private WellnessSuggestionPayload parseAiResponseToWellness(AiBrainResponse aiResponse, 
                                                              WellnessCheckinRequest originalRequest) {
        String aiText = aiResponse.getReplyText();
        
        String suggestion = extractSuggestion(aiText);
        String note = extractNote(aiText);
        String actionItems = extractActionItems(aiText);

        return WellnessSuggestionPayload.builder()
                .suggestion(suggestion)
                .note(note)
                .actionItems(actionItems)
                .aiGenerated(true)
                .emotion(aiResponse.getEmotion())
                .mascotAction(aiResponse.getMascotAction())
                .build();
    }

    private String extractSuggestion(String aiText) {
        // Look for suggestion patterns
        if (aiText.toLowerCase().contains("suggest") || aiText.toLowerCase().contains("try")) {
            String[] sentences = aiText.split("\\.");
            for (String sentence : sentences) {
                if (sentence.toLowerCase().contains("suggest") || 
                    sentence.toLowerCase().contains("try")) {
                    return sentence.trim() + ".";
                }
            }
        }
        
        // Fallback: return first meaningful sentence
        String[] sentences = aiText.split("\\.");
        return sentences.length > 0 ? sentences[0].trim() + "." : aiText;
    }

    private String extractNote(String aiText) {
        // Look for supportive or encouraging content
        if (aiText.toLowerCase().contains("remember") || aiText.toLowerCase().contains("know")) {
            String[] sentences = aiText.split("\\.");
            for (String sentence : sentences) {
                if (sentence.toLowerCase().contains("remember") || 
                    sentence.toLowerCase().contains("know")) {
                    return sentence.trim() + ".";
                }
            }
        }
        
        return "You're doing your best, and that's what matters most.";
    }

    private String extractActionItems(String aiText) {
        // Look for actionable steps
        if (aiText.toLowerCase().contains("step") || aiText.toLowerCase().contains("do")) {
            String[] sentences = aiText.split("\\.");
            for (String sentence : sentences) {
                if (sentence.toLowerCase().contains("step") || 
                    sentence.toLowerCase().contains("do")) {
                    return sentence.trim() + ".";
                }
            }
        }
        
        return "Take small, consistent actions to support your wellbeing.";
    }

    private WellnessSuggestionPayload buildFallbackWellnessResponse(WellnessCheckinRequest request) {
        log.info("Building fallback wellness response for user: {}", request.getUserId());

        String noteLower = request.getMoodNote().toLowerCase();
        String suggestion;
        String note;
        MascotAction mascotAction;
        Emotion emotion;

        if (noteLower.contains("stressed") || noteLower.contains("anxious") || noteLower.contains("overwhelmed")) {
            suggestion = "Take a short break, stretch, and focus on slow breathing for a few minutes.";
            note = "You are not alone. Small steps are okay.";
            mascotAction = MascotAction.DEFEAT; // Uses defeat.fbx (sympathetic posture)
            emotion = Emotion.SAD;
        } else if (noteLower.contains("tired")) {
            suggestion = "Drink some water, look away from the screen, and rest your eyes for 2–3 minutes.";
            note = "Gentle rest can help you focus better afterwards.";
            mascotAction = MascotAction.BREATHING_ANIMATION;
            emotion = Emotion.CALM;
        } else if (noteLower.contains("great") || noteLower.contains("amazing") || noteLower.contains("awesome")) {
            suggestion = "That's fantastic! Keep up the positive momentum!";
            note = "Your hard work is paying off beautifully.";
            mascotAction = MascotAction.VICTORY; // Uses victory.fbx
            emotion = Emotion.CELEBRATORY;
        } else if (noteLower.contains("thank") || noteLower.contains("grateful")) {
            suggestion = "Gratitude is a powerful practice for wellbeing.";
            note = "Your appreciation creates positive energy around you.";
            mascotAction = MascotAction.THANKFUL; // Uses thankful.fbx
            emotion = Emotion.HAPPY;
        } else if (noteLower.contains("angry") || noteLower.contains("frustrated") || noteLower.contains("mad")) {
            suggestion = "It's okay to feel angry. Let's channel this energy constructively.";
            note = "Your feelings are valid and temporary.";
            mascotAction = MascotAction.ANGRY; // Uses angrypoint.fbx
            emotion = Emotion.SERIOUS;
        } else if (noteLower.contains("sad") || noteLower.contains("depressed") || noteLower.contains("down")) {
            suggestion = "Be gentle with yourself. This feeling will pass.";
            note = "You're stronger than you realize.";
            mascotAction = MascotAction.SAD; // Uses sadidle.fbx
            emotion = Emotion.SUPPORTIVE;
        } else {
            suggestion = "Keep a simple plan for the next hour and reward yourself with a small break.";
            note = "Consistent small actions are powerful.";
            mascotAction = MascotAction.WAVE; // Uses wave.fbx (friendly greeting)
            emotion = Emotion.FRIENDLY;
        }

        return WellnessSuggestionPayload.builder()
                .suggestion(suggestion)
                .note(note)
                .actionItems("Practice this suggestion and notice how you feel.")
                .aiGenerated(false)
                .mascotAction(mascotAction)
                .emotion(emotion)
                .build();
    }

    private WellnessSuggestionPayload buildFallbackWellnessTips(String currentMood) {
        String moodLower = currentMood.toLowerCase();
        MascotAction mascotAction;
        Emotion emotion;
        
        if (moodLower.contains("happy") || moodLower.contains("good")) {
            mascotAction = MascotAction.VICTORY;
            emotion = Emotion.CELEBRATORY;
        } else if (moodLower.contains("sad") || moodLower.contains("down")) {
            mascotAction = MascotAction.THANKFUL;
            emotion = Emotion.SUPPORTIVE;
        } else if (moodLower.contains("stressed") || moodLower.contains("anxious")) {
            mascotAction = MascotAction.BREATHING_ANIMATION;
            emotion = Emotion.CALM;
        } else {
            mascotAction = MascotAction.WAVE;
            emotion = Emotion.FRIENDLY;
        }
        
        return WellnessSuggestionPayload.builder()
                .suggestion(String.format("For %s feelings, try gentle movement and mindful breathing.", currentMood))
                .note("Your wellbeing matters and deserves attention.")
                .actionItems("Take 5 minutes to practice this wellness tip.")
                .aiGenerated(false)
                .mascotAction(mascotAction)
                .emotion(emotion)
                .build();
    }

    private WellnessSuggestionPayload buildFallbackStressManagement(String stressLevel) {
        String stressLower = stressLevel.toLowerCase();
        MascotAction mascotAction;
        Emotion emotion;
        
        if (stressLower.contains("high") || stressLower.contains("severe")) {
            mascotAction = MascotAction.DEFEAT; // Uses defeat.fbx (sympathetic)
            emotion = Emotion.SUPPORTIVE;
        } else if (stressLower.contains("medium") || stressLower.contains("moderate")) {
            mascotAction = MascotAction.BREATHING_ANIMATION;
            emotion = Emotion.CALM;
        } else {
            mascotAction = MascotAction.WAVE;
            emotion = Emotion.FRIENDLY;
        }
        
        return WellnessSuggestionPayload.builder()
                .suggestion(String.format("For %s stress, practice the 4-7-8 breathing technique.", stressLevel))
                .note("Stress management is a skill that improves with practice.")
                .actionItems("Use this technique whenever you feel overwhelmed.")
                .aiGenerated(false)
                .mascotAction(mascotAction)
                .emotion(emotion)
                .build();
    }
}




