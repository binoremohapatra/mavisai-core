package com.studentlifeos.service;

import com.studentlifeos.dto.*;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodingAssistanceService {

    private final AiBrainWebClient aiBrainWebClient;
    private final DeviceManagementService deviceManagementService;

    public CodingAssistanceResponse processAssistanceRequest(CodingAssistanceRequest request) {
        log.info("Processing coding assistance request for user={}, device={}",
                request.getUserId(), request.getDeviceId());

        if (!deviceManagementService.isDevicePaired(request.getUserId(), request.getDeviceId())) {
            return buildErrorResponse(
                    "Your device is not paired yet. Please pair it first.",
                    request.getSessionId()
            );
        }

        try {
            CodingAssistanceRequest.AssistanceType assistanceType = determineAssistanceType(request);

            AiBrainRequest aiRequest = AiBrainRequest.builder()
                    .intent(mapIntent(assistanceType))
                    .userMessage(
                            StringUtils.hasText(request.getVoiceCommand())
                                    ? request.getVoiceCommand()
                                    : "Help me with my code"
                    )
                    .language("en")
                    .desiredTone("friendly")
                    .facts(buildFacts(request, assistanceType))
                    .build();

            log.debug("Sending AI request: {}", aiRequest);

            AiBrainResponse aiResponse = aiBrainWebClient.generate(aiRequest);

            if (aiResponse == null || !StringUtils.hasText(aiResponse.getReplyText())) {
                log.warn("AI returned empty response");
                return buildErrorResponse(
                        "I could not analyze that properly. Please try again.",
                        request.getSessionId()
                );
            }

            return buildCodingResponse(request, aiResponse, assistanceType);

        } catch (Exception e) {
            log.error("AI coding assistance failed", e);
            return buildErrorResponse(
                    "I'm having trouble helping right now. Please try again shortly.",
                    request.getSessionId()
            );
        }
    }

    // ----------------- helpers -----------------

    private Map<String, Object> buildFacts(
            CodingAssistanceRequest request,
            CodingAssistanceRequest.AssistanceType type
    ) {
        return Map.of(
                "assistanceType", type.name(),
                "language", defaultString(request.getLanguage(), "unknown"),
                "fileName", defaultString(request.getFileName(), ""),
                "codeContext", defaultString(request.getCodeContext(), "")
        );
    }

    private String mapIntent(CodingAssistanceRequest.AssistanceType type) {
        return switch (type) {
            case BUG_FIX, CODE_REVIEW -> "EXAM_QUERY";
            case CODE_GENERATION -> "STUDY_PLANNING";
            case EXPLANATION -> "CAREER_QUERY";
            case REFACTORING, OPTIMIZATION -> "CAREER_QUERY";
        };
    }

    private CodingAssistanceRequest.AssistanceType determineAssistanceType(
            CodingAssistanceRequest request
    ) {
        String cmd = request.getVoiceCommand();
        if (!StringUtils.hasText(cmd)) {
            return CodingAssistanceRequest.AssistanceType.CODE_REVIEW;
        }

        String lower = cmd.toLowerCase();
        if (lower.contains("fix") || lower.contains("bug")) return CodingAssistanceRequest.AssistanceType.BUG_FIX;
        if (lower.contains("generate") || lower.contains("write")) return CodingAssistanceRequest.AssistanceType.CODE_GENERATION;
        if (lower.contains("explain")) return CodingAssistanceRequest.AssistanceType.EXPLANATION;
        if (lower.contains("refactor") || lower.contains("optimize")) return CodingAssistanceRequest.AssistanceType.REFACTORING;

        return CodingAssistanceRequest.AssistanceType.CODE_REVIEW;
    }

    private CodingAssistanceResponse buildCodingResponse(
            CodingAssistanceRequest request,
            AiBrainResponse aiResponse,
            CodingAssistanceRequest.AssistanceType assistanceType
    ) {

        CodingAssistanceResponse.CodingAssistanceResponseBuilder builder =
                CodingAssistanceResponse.builder()
                        .sessionId(request.getSessionId())
                        .spokenResponse(aiResponse.getReplyText())
                        .voiceMeta(aiResponse.getVoiceMeta())
                        .emotion(aiResponse.getEmotion())
                        .mascotAction(aiResponse.getMascotAction())
                        .responseType(mapResponseType(assistanceType));

        extractCodeSuggestions(aiResponse.getReplyText(), builder);
        return builder.build();
    }

    private CodingAssistanceResponse.ResponseType mapResponseType(
            CodingAssistanceRequest.AssistanceType type
    ) {
        return switch (type) {
            case BUG_FIX -> CodingAssistanceResponse.ResponseType.ERROR_IDENTIFICATION;
            case CODE_GENERATION -> CodingAssistanceResponse.ResponseType.CODE_GENERATION;
            case EXPLANATION -> CodingAssistanceResponse.ResponseType.EXPLANATION;
            case REFACTORING, OPTIMIZATION -> CodingAssistanceResponse.ResponseType.REFACTORING_SUGGESTION;
            default -> CodingAssistanceResponse.ResponseType.VERBAL_GUIDANCE;
        };
    }

    private void extractCodeSuggestions(
            String reply,
            CodingAssistanceResponse.CodingAssistanceResponseBuilder builder
    ) {
        if (reply == null || !reply.contains("```")) return;

        String[] parts = reply.split("```");
        for (int i = 1; i < parts.length; i += 2) {
            String code = parts[i].trim();
            if (!code.isEmpty()) {
                builder.codeSuggestion(code);
                break;
            }
        }
    }

    private CodingAssistanceResponse buildErrorResponse(String msg, String sessionId) {
        return CodingAssistanceResponse.builder()
                .sessionId(sessionId)
                .spokenResponse(msg)
                .responseType(CodingAssistanceResponse.ResponseType.VERBAL_GUIDANCE)
                .voiceMeta(VoiceMeta.calm())
                .emotion(Emotion.CALM)
                .mascotAction(MascotAction.BREATHING_ANIMATION)
                .build();
    }

    private String defaultString(String value, String def) {
        return StringUtils.hasText(value) ? value : def;
    }
}
