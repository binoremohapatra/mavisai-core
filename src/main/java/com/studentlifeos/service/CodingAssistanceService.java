package com.studentlifeos.service;

import com.studentlifeos.dto.*;
import com.studentlifeos.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CodingAssistanceService {

    private final AIClientService aiClientService;
    private final DeviceManagementService deviceManagementService;

    public CodingAssistanceResponse processAssistanceRequest(CodingAssistanceRequest request) {
        if (!deviceManagementService.isDevicePaired(request.getUserId(), request.getDeviceId())) {
            return CodingAssistanceResponse.builder().spokenResponse("Device not paired.").build();
        }

        AiBrainRequest aiRequest = AiBrainRequest.builder()
                .intent("CODING_HELP")
                .userMessage(request.getVoiceCommand())
                .facts(Map.of(
                    "language", request.getLanguage(),
                    "codeContext", request.getCodeContext(),
                    "assistanceType", request.getAssistanceType().name()
                ))
                .desiredTone("helpful")
                .build();

        AiBrainResponse aiResponse = aiClientService.generate(aiRequest);

        return CodingAssistanceResponse.builder()
                .sessionId(request.getSessionId())
                .spokenResponse(aiResponse.getReplyText())
                .codeSuggestion(aiResponse.getCodeData() != null ? aiResponse.getCodeData().getCode() : null)
                .emotion(aiResponse.getEmotion())
                .mascotAction(aiResponse.getMascotAction())
                .voiceMeta(aiResponse.getVoiceMeta())
                .responseType(CodingAssistanceResponse.ResponseType.CODE_GENERATION)
                .build();
    }
}
