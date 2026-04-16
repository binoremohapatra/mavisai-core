package com.studentlifeos.dto;

import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WellnessSuggestionPayload {
    private String suggestion;
    private String note;
    private String actionItems;
    private Boolean aiGenerated;
    private Emotion emotion;
    private MascotAction mascotAction;
}




