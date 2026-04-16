package com.studentlifeos.dto;

import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcademicPlanPayload {
    private String focusArea;
    private String todayPlan;
    private String weeklyGoals;
    private String studyTips;
    private Boolean aiGenerated;
    private Emotion emotion;
    private MascotAction mascotAction;
}




