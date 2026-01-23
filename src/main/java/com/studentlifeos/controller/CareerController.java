package com.studentlifeos.controller;

import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.CareerAnalysisPayload;
import com.studentlifeos.dto.CareerResumeAnalyzeRequest;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.service.CareerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    @PostMapping("/resume/analyze")
    public ResponseEntity<ApiResponse<CareerAnalysisPayload>> analyze(
            @Valid @RequestBody CareerResumeAnalyzeRequest request) {
        CareerAnalysisPayload payload = careerService.analyzeResume(request);
        ApiResponse<CareerAnalysisPayload> resp = ApiResponse.of(
                "Here is a quick resume tip. " + payload.getSkillGap(),
                IntentType.CAREER_QUERY,
                MascotAction.THINKING,
                Emotion.HELPFUL,
                "Indicate the career or resume section visually without auto-navigation",
                VoiceMeta.friendly(),
                payload
        );
        return ResponseEntity.ok(resp);
    }
}


