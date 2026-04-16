package com.studentlifeos.dto;

import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private String replyText;
    private IntentType intent;
    private MascotAction mascotAction;
    private Emotion emotion;
    private String title;
    private VoiceMeta voiceMeta;
    private T data;
    private boolean success;

    // ✅ ADDED THIS FIELD to fix the compilation error
    private String audioBase64; 

    // ✅ EXISTING METHODS
    public static <T> ApiResponse<T> of(String replyText, IntentType intent, MascotAction mascotAction, 
                                      Emotion emotion, String title, VoiceMeta voiceMeta, T data) {
        return ApiResponse.<T>builder()
                .replyText(replyText)
                .intent(intent)
                .mascotAction(mascotAction)
                .emotion(emotion)
                .title(title)
                .voiceMeta(voiceMeta)
                .data(data)
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .replyText(message)
                .intent(IntentType.UNKNOWN)
                .mascotAction(MascotAction.IDLE)
                .emotion(Emotion.SERIOUS)
                .title("Error")
                .voiceMeta(null)
                .data(null)
                .success(false)
                .build();
    }
}


