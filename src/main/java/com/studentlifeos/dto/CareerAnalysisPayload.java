package com.studentlifeos.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CareerAnalysisPayload {
    private String summary;
    private String skillGap;
}




