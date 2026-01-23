package com.studentlifeos.service;

import com.studentlifeos.dto.*;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Central "OS kernel"-like orchestrator.
 * - Detects intent using simple keywords.
 * - Delegates to module services.
 * - Combines logic + AI flavor text into ApiResponse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestratorService {

    private final AttendanceService attendanceService;
    private final WellnessService wellnessService;
    private final AcademicService academicService;
    private final CareerService careerService;
    private final AiBrainWebClient aiBrainWebClient;

    public ApiResponse<?> handleChat(AIChatRequest request) {
        String message = request.getMessage();
        String lower = message.toLowerCase();
        IntentType intent = detectIntent(lower);

        log.info("Routing message '{}' with detected intent {}", message, intent);

        return switch (intent) {
            case GREETING -> buildAiResponse(
                    request,
                    intent,
                    Map.of(),
                    "Hi. I am here to help. You can ask about attendance, study planning, wellness, or career.",
                    MascotAction.WAVE_HELLO,
                    Emotion.FRIENDLY,
                    VoiceMeta.friendly(),
                    "Show a friendly wave animation near the chat bubble",
                    null
            );
            case ATTENDANCE_QUERY -> {
                AttendanceStatusPayload payload = attendanceService.getStatus(request.getUserId());
                String pct = String.format("%.0f", payload.getCurrentPercentage());
                String reply;
                MascotAction action;
                Emotion emotion;
                VoiceMeta voiceMeta;

                if (payload.getStatus().name().equals("CRITICAL")) {
                    reply = "Your attendance is at " + pct + " percent. That is risky. Please attend the next few classes.";
                    action = MascotAction.SERIOUS_WARNING;
                    emotion = Emotion.SERIOUS;
                    voiceMeta = VoiceMeta.serious();
                } else if (payload.getStatus().name().equals("WARNING")) {
                    reply = "Your attendance is at " + pct + " percent. It is getting close to the limit. Try not to miss the next class.";
                    action = MascotAction.SERIOUS_WARNING;
                    emotion = Emotion.HELPFUL;
                    voiceMeta = VoiceMeta.serious();
                } else {
                    reply = "Your attendance is at " + pct + " percent. You are currently safe. You can miss " + payload.getRemainingAbsences() + " more classes.";
                    action = MascotAction.CELEBRATE;
                    emotion = Emotion.HAPPY;
                    voiceMeta = VoiceMeta.celebratory();
                }

                Map<String, Object> facts = Map.of(
                        "attendancePercentage", payload.getCurrentPercentage(),
                        "status", payload.getStatus().name(),
                        "remainingAbsences", payload.getRemainingAbsences()
                );
                yield buildAiResponse(
                        request,
                        intent,
                        facts,
                        reply,
                        action,
                        emotion,
                        voiceMeta,
                        "Highlight attendance button or section for the user",
                        payload
                );
            }
            case EXAM_QUERY -> buildAiResponse(
                    request,
                    intent,
                    Map.of(),
                    "For your exam, keep it simple. Revise, practice, and take short breaks.",
                    MascotAction.THINKING,
                    Emotion.HELPFUL,
                    VoiceMeta.friendly(),
                    "Highlight academic/study planning section",
                    null
            );
            case STUDY_PLANNING -> {
                AcademicPlanPayload payload = academicService.createStudyPlan(
                        buildDefaultStudyPlanRequest(request));
                Map<String, Object> facts = Map.of(
                        "focusArea", payload.getFocusArea(),
                        "todayPlan", payload.getTodayPlan()
                );
                yield buildAiResponse(
                        request,
                        intent,
                        facts,
                        "Here is a simple plan for today. " + payload.getTodayPlan(),
                        MascotAction.THINKING,
                        Emotion.HELPFUL,
                        VoiceMeta.friendly(),
                        "Guide the user towards the study planning screen",
                        payload
                );
            }
            case CAREER_QUERY -> {
                CareerAnalysisPayload payload = careerService.analyzeResume(
                        buildDefaultCareerRequest(request));
                Map<String, Object> facts = Map.of(
                        "summary", payload.getSummary(),
                        "skillGap", payload.getSkillGap()
                );
                yield buildAiResponse(
                        request,
                        intent,
                        facts,
                        "Here is a quick resume tip. " + payload.getSkillGap(),
                        MascotAction.THINKING,
                        Emotion.HELPFUL,
                        VoiceMeta.friendly(),
                        "Gently highlight the career tab or resume upload area",
                        payload
                );
            }
            case WELLNESS_CHECKIN -> {
                WellnessSuggestionPayload payload = wellnessService.checkin(
                        buildWellnessRequest(request));
                Map<String, Object> facts = Map.of(
                        "suggestion", payload.getSuggestion(),
                        "note", payload.getNote()
                );
                yield buildAiResponse(
                        request,
                        intent,
                        facts,
                        payload.getSuggestion(),
                        MascotAction.BREATHING_ANIMATION,
                        Emotion.CALM,
                        VoiceMeta.calm(),
                        "Show a calm breathing animation near the mascot",
                        payload
                );
            }
            case UNKNOWN -> buildAiResponse(
                    request,
                    intent,
                    Map.of(),
                    "I am not sure I understood. You can ask about attendance, study planning, wellness, or career.",
                    MascotAction.THINKING,
                    Emotion.SERIOUS,
                    VoiceMeta.serious(),
                    "Show a small thinking animation; no automatic navigation",
                    null
            );
        };
    }

    private IntentType detectIntent(String lowerMessage) {
        if (!StringUtils.hasText(lowerMessage)) {
            return IntentType.UNKNOWN;
        }
        if (lowerMessage.contains("hi") || lowerMessage.contains("hello") || lowerMessage.contains("hey")) {
            return IntentType.GREETING;
        }
        if (lowerMessage.contains("attendance") || lowerMessage.contains("bunk") || lowerMessage.contains("present")) {
            return IntentType.ATTENDANCE_QUERY;
        }
        if (lowerMessage.contains("exam") || lowerMessage.contains("test") || lowerMessage.contains("paper")) {
            return IntentType.EXAM_QUERY;
        }
        if (lowerMessage.contains("study plan") || lowerMessage.contains("timetable") || lowerMessage.contains("schedule")) {
            return IntentType.STUDY_PLANNING;
        }
        if (lowerMessage.contains("job") || lowerMessage.contains("internship") || lowerMessage.contains("career") ||
                lowerMessage.contains("resume") || lowerMessage.contains("cv")) {
            return IntentType.CAREER_QUERY;
        }
        if (lowerMessage.contains("stressed") || lowerMessage.contains("anxious") ||
                lowerMessage.contains("tired") || lowerMessage.contains("mood")) {
            return IntentType.WELLNESS_CHECKIN;
        }
        return IntentType.UNKNOWN;
    }

    private AcademicStudyPlanRequest buildDefaultStudyPlanRequest(AIChatRequest request) {
        AcademicStudyPlanRequest dto = new AcademicStudyPlanRequest();
        dto.setUserId(request.getUserId());
        dto.setFocusArea("Core subjects");
        return dto;
    }

    private CareerResumeAnalyzeRequest buildDefaultCareerRequest(AIChatRequest request) {
        CareerResumeAnalyzeRequest dto = new CareerResumeAnalyzeRequest();
        dto.setUserId(request.getUserId());
        dto.setResumeText("Short summary from chat: " + request.getMessage());
        return dto;
    }

    private WellnessCheckinRequest buildWellnessRequest(AIChatRequest request) {
        WellnessCheckinRequest dto = new WellnessCheckinRequest();
        dto.setUserId(request.getUserId());
        dto.setMoodNote(request.getMessage());
        return dto;
    }

    private <T> ApiResponse<T> buildAiResponse(
            AIChatRequest request,
            IntentType intent,
            Map<String, Object> facts,
            String fallbackReply,
            MascotAction fallbackAction,
            Emotion fallbackEmotion,
            VoiceMeta fallbackVoiceMeta,
            String uiHint,
            T payload
    ) {
        AiBrainRequest aiRequest = AiBrainRequest.builder()
                .intent(intent.name())
                .facts(facts)
                .userMessage(request.getMessage())
                .desiredTone(fallbackVoiceMeta != null ? fallbackVoiceMeta.getTone() : "friendly")
                .language("en")
                .build();
        AiBrainResponse aiResponse = aiBrainWebClient.generate(aiRequest);

        String replyText = fallbackReply;
        MascotAction action = fallbackAction;
        Emotion emotion = fallbackEmotion;
        VoiceMeta voiceMeta = fallbackVoiceMeta;

        if (aiResponse != null) {
            if (StringUtils.hasText(aiResponse.getReplyText())) {
                replyText = aiResponse.getReplyText();
            }
            if (aiResponse.getMascotAction() != null) {
                action = aiResponse.getMascotAction();
            }
            if (aiResponse.getEmotion() != null) {
                emotion = aiResponse.getEmotion();
            }
            if (aiResponse.getVoiceMeta() != null) {
                voiceMeta = aiResponse.getVoiceMeta();
            }
        }

        return ApiResponse.of(
                replyText,
                intent,
                action,
                emotion,
                uiHint,
                voiceMeta,
                payload
        );
    }
}


