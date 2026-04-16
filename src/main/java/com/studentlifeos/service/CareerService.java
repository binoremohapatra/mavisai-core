package com.studentlifeos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import com.studentlifeos.dto.CareerAnalysisPayload;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerService {

    private final AIClientService aiClientService;
    private final ObjectMapper objectMapper;

    // Backup Dictionary
    private static final Map<String, String> ROLE_MAP = Map.ofEntries(
        Map.entry("react", "Frontend Developer"),
        Map.entry("front", "Frontend Developer"),
        Map.entry("java", "Backend Engineer"),
        Map.entry("spring", "Backend Engineer"),
        Map.entry("node", "Backend Engineer"),
        Map.entry("dev", "Software Engineer"),
        Map.entry("code", "Software Engineer"),
        Map.entry("doc", "Medical Practitioner"),
        Map.entry("med", "Medical Practitioner"),
        Map.entry("pilot", "Airline Pilot"),
        Map.entry("mech", "Mechanical Engineer"),
        Map.entry("cook", "Chef"),
        Map.entry("teach", "Educator")
    );

    public CareerAnalysisPayload analyzeResume(String resumeText, String userId) {
        log.info("Generating Career Diagnostics via AI for User: {}", userId);

        CareerAnalysisPayload payload = null;

        // 1. Try AI First (Network Call)
        try {
            // Prompt ko thoda simple kiya hai taaki tokens kam use ho aur error na aaye
            String prompt = String.format(
                "Analyze this resume. Return valid JSON only. No markdown.\n" +
                "Resume: \"%s\"\n" +
                "Format: { \"role\": \"Job Title\", \"matchScore\": 85, \"summary\": \"1 line text\", \"recommendations\": \"1 line text\", \"skillGaps\": [{\"skill\": \"Name\", \"current\": 50}] }", 
                resumeText.substring(0, Math.min(resumeText.length(), 1500)) // Limit text to save tokens
            );

            AiBrainRequest request = AiBrainRequest.builder()
                    .intent("CAREER_ANALYSIS")
                    .userMessage(prompt)
                    .facts(Map.of("userId", userId))
                    .desiredTone("analytical")
                    .language("en")
                    .build();

            AiBrainResponse response = aiClientService.generate(request);
            
            if (response != null && response.getReplyText() != null) {
                // CRITICAL FIX: Single quotes ko Double quotes me badal raha hu
                String rawJson = cleanJson(response.getReplyText());
                log.info("AI JSON (Cleaned): {}", rawJson);
                payload = objectMapper.readValue(rawJson, CareerAnalysisPayload.class);
            }

        } catch (Exception e) {
            log.warn("AI Issue: {}. Switching to Smart Backup.", e.getMessage());
        }

        // 2. FALLBACK (Sirf tab chalega jab AI fail ho)
        if (payload == null || payload.getRole().equalsIgnoreCase("General Application")) {
            payload = runKeywordAnalysis(resumeText);
        }

        updateMascotState(payload);
        return payload;
    }

    // MAGIC CLEANER: Ye AI ki gandi formatting (Single Quotes) ko fix karega
    private String cleanJson(String text) {
        if (text == null) return "{}";
        
        // 1. Remove Markdown
        String cleaned = text.replace("```json", "").replace("```", "").trim();
        
        // 2. Fix Single Quotes (Example: 'role' -> "role")
        // Note: Ye perfect nahi hai par 99% cases me kaam karega
        if (!cleaned.contains("\"role\"") && cleaned.contains("'role'")) {
            cleaned = cleaned.replace("'", "\"");
        }
        
        // 3. Extract JSON Object only
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start != -1 && end != -1) return cleaned.substring(start, end + 1);
        
        return cleaned; 
    }

    private void updateMascotState(CareerAnalysisPayload analysis) {
        analysis.setAiGenerated(true);
        if (analysis.getMatchScore() > 80) {
            analysis.setMascotAction(MascotAction.VICTORY);
            analysis.setEmotion(Emotion.HAPPY);
        } else {
            analysis.setMascotAction(MascotAction.THINKING);
            analysis.setEmotion(Emotion.SERIOUS);
        }
    }

    private CareerAnalysisPayload runKeywordAnalysis(String text) {
        String lower = text.toLowerCase();
        String detectedRole = "General Professional";
        int matchScore = 60;

        for (Map.Entry<String, String> entry : ROLE_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                detectedRole = entry.getValue();
                matchScore = 88;
                break; 
            }
        }

        List<Map<String, Object>> skills = new ArrayList<>();
        skills.add(Map.of("skill", "Core Skills", "current", 75));
        skills.add(Map.of("skill", "Experience", "current", 60));

        return CareerAnalysisPayload.builder()
                .role(detectedRole)
                .matchScore(matchScore)
                .skillGaps(skills)
                .summary("AI is busy, but based on keywords, you are a " + detectedRole + ".")
                .recommendations("Try again in 30 seconds for a full AI analysis.")
                .build();
    }
}