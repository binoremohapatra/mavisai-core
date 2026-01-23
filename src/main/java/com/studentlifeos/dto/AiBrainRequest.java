package com.studentlifeos.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@Builder
public class AiBrainRequest {
    @NonNull
    private String intent;
    @NonNull
    private Map<String, Object> facts;
    @NonNull
    private String userMessage;
    @NonNull
    private String desiredTone;
    @NonNull
    private String language;
}
