package com.studentlifeos.dto;

import com.studentlifeos.enums.AttendanceStatusType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceStatusPayload {
    private AttendanceStatusType status;
    private double currentPercentage;
    private int totalClasses;
    private int attendedClasses;
    private int maxAbsencesAllowed;
    private int remainingAbsences;
}




