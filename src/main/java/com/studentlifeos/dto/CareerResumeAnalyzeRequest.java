package com.studentlifeos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CareerResumeAnalyzeRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String resumeText;
    
    private String jobTitle;
    private String experienceLevel;
}




