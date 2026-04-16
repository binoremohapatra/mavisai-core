package com.studentlifeos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiBrainResponse {
    private String replyText;
    private Emotion emotion;
    private MascotAction mascotAction;
    private VoiceMeta voiceMeta;
    private CodeData codeData; // ✅ Critical for Coding Service

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodeData {
        private String code;
        private String language;
        private String action; // OVERWRITE, HIGHLIGHT, etc.
    }
}
