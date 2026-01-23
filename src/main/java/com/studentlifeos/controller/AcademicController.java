package com.studentlifeos.controller;

import com.studentlifeos.dto.AcademicPlanPayload;
import com.studentlifeos.dto.AcademicStudyPlanRequest;
import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.service.AcademicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;

    @PostMapping("/study-plan")
    public ResponseEntity<ApiResponse<AcademicPlanPayload>> createPlan(
            @Valid @RequestBody AcademicStudyPlanRequest request) {
        AcademicPlanPayload payload = academicService.createStudyPlan(request);
        ApiResponse<AcademicPlanPayload> resp = ApiResponse.of(
                "Here is your plan for today. " + payload.getTodayPlan(),
                IntentType.STUDY_PLANNING,
                MascotAction.THINKING,
                Emotion.HELPFUL,
                "Emphasize the study planning section so the user can review the plan",
                VoiceMeta.friendly(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/today-plan")
    public ResponseEntity<ApiResponse<AcademicPlanPayload>> todayPlan(@RequestParam Long userId) {
        AcademicPlanPayload payload = academicService.getTodayPlan(userId);
        ApiResponse<AcademicPlanPayload> resp = ApiResponse.of(
                "Here is your plan for today. " + payload.getTodayPlan(),
                IntentType.STUDY_PLANNING,
                MascotAction.CELEBRATE,
                Emotion.SUPPORTIVE,
                "Subtly highlight today's plan card",
                VoiceMeta.friendly(),
                payload
        );
        return ResponseEntity.ok(resp);
    }
}


