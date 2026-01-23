package com.studentlifeos.dto;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.List;

@Data
@Builder
public class CodingAssistanceRequest {
    
    @NotBlank
    private String userId;
    
    @NotBlank
    private String deviceId;
    
    @NotBlank
    private String sessionId;
    
    private String hotword; // e.g., "Hey Student", "Code Assistant"
    
    private String voiceCommand; // Transcribed voice input
    
    private String codeContext; // Current code snippet from IDE
    
    private String language; // Programming language (java, python, javascript, etc.)
    
    private String fileName; // Current file being edited
    
    private Integer cursorPosition; // Cursor position in code
    
    private List<String> imports; // Current imports in file
    
    private Map<String, Object> ideState; // IDE context (open files, breakpoints, etc.)
    
    private AssistanceType assistanceType;
    
    public enum AssistanceType {
        CODE_REVIEW,
        BUG_FIX,
        CODE_GENERATION,
        EXPLANATION,
        REFACTORING,
        OPTIMIZATION
    }
}
