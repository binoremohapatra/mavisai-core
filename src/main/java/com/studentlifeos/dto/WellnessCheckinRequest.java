package com.studentlifeos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WellnessCheckinRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String moodNote; // free-text description, e.g. "tired but okay"
}




