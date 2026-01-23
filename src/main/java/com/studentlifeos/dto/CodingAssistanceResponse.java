package com.studentlifeos.dto;

import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CodingAssistanceResponse {
    
    @NonNull
    private String sessionId;
    
    @NonNull
    private String spokenResponse; // Text-to-speech output
    
    private String codeSuggestion; // Generated/corrected code
    
    private List<String> codeChanges; // Specific changes to make
    
    private List<String> issues; // Identified issues in code
    
    private List<String> suggestions; // Improvement suggestions
    
    private String explanation; // Detailed explanation
    
    @NonNull
    private VoiceMeta voiceMeta; // Voice metadata for TTS
    
    @NonNull
    private Emotion emotion; // Emotion for mascot animation
    
    @NonNull
    private MascotAction mascotAction; // Mascot animation
    
    private Map<String, Object> metadata; // Additional context
    
    @NonNull
    private ResponseType responseType;
    
    public enum ResponseType {
        VERBAL_GUIDANCE,    // Spoken instructions only
        CODE_CORRECTION,    // Specific code fixes
        CODE_GENERATION,     // Generate new code
        EXPLANATION,         // Explain code concepts
        REFACTORING_SUGGESTION, // Suggest improvements
        ERROR_IDENTIFICATION // Point out bugs/issues
    }
}
