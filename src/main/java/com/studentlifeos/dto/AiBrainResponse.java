package com.studentlifeos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiBrainResponse {
    private String replyText;
    private Emotion emotion;
    private MascotAction mascotAction;
    private VoiceMeta voiceMeta;
}
