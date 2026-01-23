package com.studentlifeos.dto;

import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import lombok.Builder;
import lombok.Data;

/**
 * Standard response envelope used by all endpoints.
 *
 * AI SAFETY:
 * - Backend NEVER performs real UI actions.
 * - uiHint is descriptive only (e.g. "Highlight attendance button").
 * - Frontend / user stays in full control.
 */
@Data
@Builder
public class ApiResponse<T> {
    private String replyText;
    private IntentType detectedIntent;
    private MascotAction mascotAction;
    private Emotion emotion;
    private String uiHint;
    private VoiceMeta voiceMeta;
    private T data;

    public static <T> ApiResponse<T> of(String replyText,
                                        IntentType intent,
                                        MascotAction action,
                                        Emotion emotion,
                                        String uiHint,
                                        VoiceMeta voiceMeta,
                                        T data) {
        return ApiResponse.<T>builder()
                .replyText(replyText)
                .detectedIntent(intent)
                .mascotAction(action)
                .emotion(emotion)
                .uiHint(uiHint)
                .voiceMeta(voiceMeta)
                .data(data)
                .build();
    }
}


