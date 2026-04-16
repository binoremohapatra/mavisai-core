package com.studentlifeos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentlifeos.dto.AcademicPlanPayload;
import com.studentlifeos.dto.AcademicStudyPlanRequest;
import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicService {

    private final AIClientService aiClientService;
    private final ObjectMapper objectMapper;

    /**
     * Architecting high-intensity protocol for specific focus areas.
     */
    public AcademicPlanPayload createStudyPlan(AcademicStudyPlanRequest request) {
        log.info("Architecting protocol for focus area: {}", request.getFocusArea());
        int days = extractDays(request.getFocusArea());
        String prompt = buildPrompt(request.getFocusArea(), days);
        return callAiAndParse(request.getUserId(), "STUDY_PLANNING", prompt, request.getFocusArea());
    }

    /**
     * Retrieving 24-hour priority protocol.
     */
    public AcademicPlanPayload getTodayPlan(Long userId) {
        log.info("Retrieving 24-hour priority protocol for user: {}", userId);
        String prompt = "Act as an Academic Architect. Generate a focused study protocol for today. " +
                "Return ONLY a JSON object with keys: 'immediate', 'weekly', 'tips'.";
        return callAiAndParse(userId, "STUDY_PLANNING", prompt, "Today's Plan");
    }

    /**
     * Architecting intensive exam prep roadmap.
     */
    public AcademicPlanPayload getExamPreparationPlan(Long userId, String subject, int daysUntilExam) {
        log.info("Architecting intensive exam prep for {} | T-minus {} days", subject, daysUntilExam);
        String prompt = String.format(
                "Act as an Academic Architect. Create an intensive, high-yield exam prep protocol for %s for the next %d days. " +
                        "Return ONLY a JSON object with keys: 'immediate', 'weekly', 'tips'.",
                subject, daysUntilExam
        );
        return callAiAndParse(userId, "EXAM_QUERY", prompt, subject + " Prep");
    }

    private int extractDays(String input) {
        try {
            if (input.toLowerCase().contains("week")) return 7;
            String numeric = input.replaceAll("[^0-9]", "");
            return numeric.isEmpty() ? 30 : Integer.parseInt(numeric);
        } catch (Exception e) {
            return 30;
        }
    }

    private String buildPrompt(String area, int days) {
        return String.format(
                "Act as an elite Academic Architect. Architect a COMPREHENSIVE %d-day study protocol for '%s'.\n" +
                        "### OUTPUT RULES:\n" +
                        "1. You MUST provide a specific, unique objective for EVERY day from Day 1 to Day %d.\n" +
                        "2. Format the 'weekly' field as a clean vertical checklist using [DAY XX] :: Topic.\n" +
                        "3. Use professional, high-level terminology.\n" +
                        "4. Return ONLY a raw JSON object with these keys: 'immediate', 'weekly', 'tips'.",
                days, area, days
        );
    }

    private AcademicPlanPayload callAiAndParse(Long userId, String intent, String prompt, String focusArea) {
        AiBrainRequest aiRequest = AiBrainRequest.builder()
                .intent(intent)
                .userMessage(prompt)
                .facts(Map.of("userId", userId, "topic", focusArea))
                .desiredTone("serious")
                .language("en")
                .build();

        AiBrainResponse aiResponse = aiClientService.generate(aiRequest);
        return parseAiResponse(aiResponse, focusArea);
    }

    /**
     * ✅ HYPER-PARSER: Isolates JSON block and fixes syntax glitches.
     */
    private AcademicPlanPayload parseAiResponse(AiBrainResponse aiResponse, String focusArea) {
        if (aiResponse == null || aiResponse.getReplyText() == null) {
            return buildFallback(focusArea);
        }

        String rawText = aiResponse.getReplyText();
        log.info("Raw AI Output received: {}", rawText);

        try {
            // 1. Isolate the JSON part between { and }
            int start = rawText.indexOf('{');
            int end = rawText.lastIndexOf('}');

            if (start != -1 && end != -1 && end >= start) {
                String jsonPart = rawText.substring(start, end + 1);

                // 2. Fix common AI JSON mishaps (unescaped quotes, extra commas)
                jsonPart = jsonPart.replaceAll("(?<!\\\\)\"(?![: ,}\\]])", "\\\\\"");
                
                JsonNode json = objectMapper.readTree(jsonPart);

                return AcademicPlanPayload.builder()
                        .focusArea(focusArea)
                        .todayPlan(json.path("immediate").asText(json.path("todayPlan").asText("Analyze core logic.")))
                        .weeklyGoals(json.path("weekly").asText(""))
                        .studyTips(json.path("tips").asText("Execute with precision."))
                        .aiGenerated(true)
                        .emotion(aiResponse.getEmotion() != null ? aiResponse.getEmotion() : Emotion.HELPFUL)
                        .mascotAction(aiResponse.getMascotAction() != null ? aiResponse.getMascotAction() : MascotAction.THINKING)
                        .build();
            }
            log.warn("No JSON block found in AI response. Using text as weekly goals.");
            throw new Exception("No valid JSON block found.");
        } catch (Exception e) {
            log.error("Parsing failed: {}. Raw text: {}", e.getMessage(), rawText);
            
            // If it's not JSON, we put the full reply in weeklyGoals so the user can at least read it
            return AcademicPlanPayload.builder()
                    .focusArea(focusArea)
                    .todayPlan("Neural Sync Active. (Non-JSON Response)")
                    .weeklyGoals(rawText != null ? rawText : "No response from neural link.")
                    .studyTips("Check AI service logs for key rotation or timeout issues.")
                    .aiGenerated(true)
                    .build();
        }

    }

    private AcademicPlanPayload buildFallback(String topic) {
        return AcademicPlanPayload.builder()
                .focusArea(topic)
                .todayPlan("Neural disconnect detected.")
                .weeklyGoals("Check AI service connectivity.")
                .studyTips("Stay focused.")
                .aiGenerated(false)
                .build();
    }
}