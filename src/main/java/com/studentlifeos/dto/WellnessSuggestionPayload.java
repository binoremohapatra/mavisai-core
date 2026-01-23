package com.studentlifeos.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WellnessSuggestionPayload {
    private String suggestion;
    private String note;
}




