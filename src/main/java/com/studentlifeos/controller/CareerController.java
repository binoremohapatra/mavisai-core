package com.studentlifeos.controller;

import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.CareerAnalysisPayload;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.service.CareerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/career")
@RequiredArgsConstructor
@Slf4j
public class CareerController {

    private final CareerService careerService;

    @PostMapping("/resume/analyze")
    public ResponseEntity<ApiResponse<CareerAnalysisPayload>> analyze(@RequestBody Map<String, Object> request) {
        String resumeText = (String) request.get("resumeText");
        String userId = String.valueOf(request.getOrDefault("userId", "1"));

        if (resumeText == null || resumeText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Resume content is required"));
        }

        // Delegate complex logic to Service
        CareerAnalysisPayload analysis = careerService.analyzeResume(resumeText, userId);

        return ResponseEntity.ok(ApiResponse.of(
            analysis.getSummary(), // This will be spoken by VoiceService
            IntentType.CAREER_QUERY,
            analysis.getMascotAction(),
            analysis.getEmotion(),
            "Career Diagnostics",
            VoiceMeta.friendly(),
            analysis // The structured data for the UI
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Career service is healthy");
    }
}
