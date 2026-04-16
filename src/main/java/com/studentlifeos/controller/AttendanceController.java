package com.studentlifeos.controller;

import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.AttendanceConfigRequest;
import com.studentlifeos.dto.AttendanceStatusPayload;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/config")
    public ResponseEntity<ApiResponse<AttendanceStatusPayload>> saveConfig(
            @Valid @RequestBody AttendanceConfigRequest request) {
        log.info("Received attendance config request for user: {}", request.getUserId());
        
        AttendanceStatusPayload payload = attendanceService.saveConfig(request);
        String reply = "Saved your attendance settings. Your current status is " + payload.getStatus() + ".";
        
        ApiResponse<AttendanceStatusPayload> resp = ApiResponse.of(
                reply,
                IntentType.ATTENDANCE_QUERY,
                MascotAction.POINT_TO_BUTTON,
                Emotion.HELPFUL,
                "Highlight attendance section so the user can review details",
                VoiceMeta.friendly(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AttendanceStatusPayload>> getStatus(@RequestParam Long userId) {
        log.info("Received attendance status request for user: {}", userId);
        
        AttendanceStatusPayload payload = attendanceService.getStatus(userId);
        String pct = String.format("%.0f", payload.getCurrentPercentage());
        String reply = "Your attendance is at " + pct + " percent. Status is " + payload.getStatus() + ".";
        
        ApiResponse<AttendanceStatusPayload> resp = ApiResponse.of(
                reply,
                IntentType.ATTENDANCE_QUERY,
                MascotAction.POINT_TO_BUTTON,
                Emotion.HELPFUL,
                "Highlight attendance button or card with the summary",
                VoiceMeta.friendly(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Attendance service is healthy");
    }
}


