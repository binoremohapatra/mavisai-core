package com.studentlifeos.service;

import com.studentlifeos.domain.AttendanceConfig;
import com.studentlifeos.dto.AttendanceConfigRequest;
import com.studentlifeos.dto.AttendanceStatusPayload;
import com.studentlifeos.enums.AttendanceStatusType;
import com.studentlifeos.repository.AttendanceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceConfigRepository configRepository;

    public AttendanceStatusPayload saveConfig(AttendanceConfigRequest request) {
        AttendanceConfig config = new AttendanceConfig();
        config.setUserId(request.getUserId());
        config.setTotalClasses(request.getTotalClasses());
        config.setAttendedClasses(request.getAttendedClasses());
        config.setRequiredAttendance(request.getRequiredAttendance());
        configRepository.save(config);

        log.info("Saved attendance config for user {}", request.getUserId());
        return buildStatus(config);
    }

    public AttendanceStatusPayload getStatus(Long userId) {
        AttendanceConfig config = configRepository.findTopByUserIdOrderByIdDesc(userId)
                .orElseGet(() -> {
                    AttendanceConfig c = new AttendanceConfig();
                    c.setUserId(userId);
                    c.setTotalClasses(0);
                    c.setAttendedClasses(0);
                    c.setRequiredAttendance(75);
                    return c;
                });
        log.info("Computed attendance status for user {}", userId);
        return buildStatus(config);
    }

    /**
     * Pure rule-based attendance calculation.
     *
     * maxAbsences = totalClasses * (1 - requiredAttendance%)
     */
    private AttendanceStatusPayload buildStatus(AttendanceConfig config) {
        int total = config.getTotalClasses();
        int attended = config.getAttendedClasses();
        int required = config.getRequiredAttendance();

        double currentPct = total == 0 ? 0.0 : (attended * 100.0) / total;
        int maxAbsencesAllowed = (int) Math.floor(total * (1 - (required / 100.0)));
        int alreadyMissed = total - attended;
        int remainingAbsences = Math.max(0, maxAbsencesAllowed - alreadyMissed);

        AttendanceStatusType status;
        if (currentPct >= required) {
            status = AttendanceStatusType.SAFE;
        } else if (currentPct >= required - 5) {
            status = AttendanceStatusType.WARNING;
        } else {
            status = AttendanceStatusType.CRITICAL;
        }

        return AttendanceStatusPayload.builder()
                .status(status)
                .currentPercentage(currentPct)
                .totalClasses(total)
                .attendedClasses(attended)
                .maxAbsencesAllowed(maxAbsencesAllowed)
                .remainingAbsences(remainingAbsences)
                .build();
    }
}




