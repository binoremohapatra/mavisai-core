package com.studentlifeos.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcademicPlanPayload {
    private String todayPlan;
    private String focusArea;
}




