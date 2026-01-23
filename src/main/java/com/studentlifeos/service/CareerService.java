package com.studentlifeos.service;

import com.studentlifeos.dto.CareerAnalysisPayload;
import com.studentlifeos.dto.CareerResumeAnalyzeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stubbed career module.
 * Returns a simple, understandable skill-gap analysis.
 */
@Service
@Slf4j
public class CareerService {

    public CareerAnalysisPayload analyzeResume(CareerResumeAnalyzeRequest request) {
        log.info("Running mock career analysis for user {}", request.getUserId());

        String summary = "Your resume shows good starting experience. "
                + "Highlight projects and responsibilities more clearly.";
        String skillGap = "Add 1–2 measurable outcomes (e.g. performance, users, impact) "
                + "and mention tools/technologies for each project.";

        return CareerAnalysisPayload.builder()
                .summary(summary)
                .skillGap(skillGap)
                .build();
    }
}




