package com.studentlifeos.service;

import com.studentlifeos.dto.AcademicPlanPayload;
import com.studentlifeos.dto.AcademicStudyPlanRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stubbed academic service.
 * Returns mock but realistic study plans – no persistence to keep it hackathon-light.
 */
@Service
@Slf4j
public class AcademicService {

    public AcademicPlanPayload createStudyPlan(AcademicStudyPlanRequest request) {
        log.info("Creating mock study plan for user {} with focus {}", request.getUserId(), request.getFocusArea());
        String todayPlan = "1 hour quick revision for " + request.getFocusArea()
                + ", 30 minutes practice questions, 10 minutes recap.";
        return AcademicPlanPayload.builder()
                .focusArea(request.getFocusArea())
                .todayPlan(todayPlan)
                .build();
    }

    public AcademicPlanPayload getTodayPlan(Long userId) {
        log.info("Returning default mock today-plan for user {}", userId);
        String todayPlan = "30 minutes revising yesterday's topics, "
                + "30 minutes new concepts, and 20 minutes self-test.";
        return AcademicPlanPayload.builder()
                .focusArea("General")
                .todayPlan(todayPlan)
                .build();
    }
}




