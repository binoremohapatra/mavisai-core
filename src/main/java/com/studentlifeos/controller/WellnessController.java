package com.studentlifeos.controller;

import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.dto.WellnessCheckinRequest;
import com.studentlifeos.dto.WellnessSuggestionPayload;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.service.WellnessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wellness")
@RequiredArgsConstructor
@Slf4j
public class WellnessController {

    private final WellnessService wellnessService;

    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<WellnessSuggestionPayload>> checkin(
            @Valid @RequestBody WellnessCheckinRequest request) {
        log.info("Received wellness check-in request for user: {}", request.getUserId());
        
        WellnessSuggestionPayload payload = wellnessService.checkin(request);
        
        ApiResponse<WellnessSuggestionPayload> resp = ApiResponse.of(
                payload.getSuggestion(),
                IntentType.WELLNESS_CHECKIN,
                payload.getMascotAction() != null ? payload.getMascotAction() : MascotAction.BREATHING_ANIMATION,
                payload.getEmotion() != null ? payload.getEmotion() : Emotion.CALM,
                "Show a calm breathing or relaxing animation next to the mascot",
                VoiceMeta.calm(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/tips")
    public ResponseEntity<ApiResponse<WellnessSuggestionPayload>> getWellnessTips(
            @RequestParam Long userId,
            @RequestParam String currentMood) {
        log.info("Received wellness tips request for user: {}, mood: {}", userId, currentMood);
        
        WellnessSuggestionPayload payload = wellnessService.getWellnessTips(userId, currentMood);
        
        ApiResponse<WellnessSuggestionPayload> resp = ApiResponse.of(
                payload.getSuggestion(),
                IntentType.WELLNESS_CHECKIN,
                payload.getMascotAction() != null ? payload.getMascotAction() : MascotAction.THINKING,
                payload.getEmotion() != null ? payload.getEmotion() : Emotion.SUPPORTIVE,
                "Show wellness tips with gentle animations",
                VoiceMeta.friendly(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/stress-management")
    public ResponseEntity<ApiResponse<WellnessSuggestionPayload>> getStressManagement(
            @RequestParam Long userId,
            @RequestParam String stressLevel) {
        log.info("Received stress management request for user: {}, level: {}", userId, stressLevel);
        
        WellnessSuggestionPayload payload = wellnessService.getStressManagement(userId, stressLevel);
        
        ApiResponse<WellnessSuggestionPayload> resp = ApiResponse.of(
                payload.getSuggestion(),
                IntentType.WELLNESS_CHECKIN,
                payload.getMascotAction() != null ? payload.getMascotAction() : MascotAction.BREATHING_ANIMATION,
                payload.getEmotion() != null ? payload.getEmotion() : Emotion.CALM,
                "Show stress management techniques with breathing guide",
                VoiceMeta.calm(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Wellness service is healthy");
    }
}


