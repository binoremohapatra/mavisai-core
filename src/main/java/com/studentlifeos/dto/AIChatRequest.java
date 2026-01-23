package com.studentlifeos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AIChatRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String message;
}




