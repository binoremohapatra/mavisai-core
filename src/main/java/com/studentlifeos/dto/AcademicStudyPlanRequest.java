package com.studentlifeos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcademicStudyPlanRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String focusArea; // e.g. "DSA, OS"
}




