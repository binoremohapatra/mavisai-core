package com.studentlifeos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceConfigRequest {
    @NotNull
    private Long userId;

    @Min(1)
    private int totalClasses;

    @Min(0)
    private int attendedClasses;

    @Min(50)
    private int requiredAttendance; // e.g. 75 for 75%
}




