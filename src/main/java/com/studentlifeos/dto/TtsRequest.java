package com.studentlifeos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TtsRequest {
    @NotBlank
    private String text;
    
    private String voice = "female"; // Default to female voice
}
