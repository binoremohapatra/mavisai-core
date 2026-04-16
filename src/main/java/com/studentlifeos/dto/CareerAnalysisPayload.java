package com.studentlifeos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.MascotAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CareerAnalysisPayload {
    // Structured Data for UI Graphs
    private String role;
    private Integer matchScore;
    private List<Map<String, Object>> skillGaps; // [{"skill": "Java", "current": 80}]

    // Text Content for Voice/Reading
    private String summary;
    private String recommendations;
    
    // Metadata
    private Boolean aiGenerated;
    private Emotion emotion;
    private MascotAction mascotAction;
}