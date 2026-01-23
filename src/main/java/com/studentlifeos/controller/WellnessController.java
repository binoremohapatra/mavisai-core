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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessService wellnessService;

    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<WellnessSuggestionPayload>> checkin(
            @Valid @RequestBody WellnessCheckinRequest request) {
        WellnessSuggestionPayload payload = wellnessService.checkin(request);
        ApiResponse<WellnessSuggestionPayload> resp = ApiResponse.of(
                payload.getSuggestion(),
                IntentType.WELLNESS_CHECKIN,
                MascotAction.BREATHING_ANIMATION,
                Emotion.CALM,
                "Show a calm breathing or relaxing animation next to the mascot",
                VoiceMeta.calm(),
                payload
        );
        return ResponseEntity.ok(resp);
    }
}


