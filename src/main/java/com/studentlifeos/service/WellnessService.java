package com.studentlifeos.service;

import com.studentlifeos.domain.WellnessCheckin;
import com.studentlifeos.dto.WellnessCheckinRequest;
import com.studentlifeos.dto.WellnessSuggestionPayload;
import com.studentlifeos.repository.WellnessCheckinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WellnessService {

    private final WellnessCheckinRepository checkinRepository;

    /**
     * Non-clinical, supportive suggestions only.
     * No diagnosis or medical language.
     */
    public WellnessSuggestionPayload checkin(WellnessCheckinRequest request) {
        WellnessCheckin entity = new WellnessCheckin();
        entity.setUserId(request.getUserId());
        entity.setMoodNote(request.getMoodNote());
        entity.setCreatedAt(LocalDateTime.now());
        checkinRepository.save(entity);

        log.info("Wellness check-in saved for user {}", request.getUserId());

        String noteLower = request.getMoodNote().toLowerCase();
        String suggestion;
        String note;

        if (noteLower.contains("stressed") || noteLower.contains("anxious") || noteLower.contains("overwhelmed")) {
            suggestion = "Take a short break, stretch, and focus on slow breathing for a few minutes.";
            note = "You are not alone. Small steps are okay.";
        } else if (noteLower.contains("tired")) {
            suggestion = "Drink some water, look away from the screen, and rest your eyes for 2–3 minutes.";
            note = "Gentle rest can help you focus better afterwards.";
        } else {
            suggestion = "Keep a simple plan for the next hour and reward yourself with a small break.";
            note = "Consistent small actions are powerful.";
        }

        return WellnessSuggestionPayload.builder()
                .suggestion(suggestion)
                .note(note)
                .build();
    }
}




