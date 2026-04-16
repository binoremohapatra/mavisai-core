package com.studentlifeos.controller;

import com.studentlifeos.dto.AcademicPlanPayload;
import com.studentlifeos.dto.AcademicStudyPlanRequest;
import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.model.Subject;
import com.studentlifeos.repository.SubjectRepository;
import com.studentlifeos.service.AcademicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic")
@RequiredArgsConstructor
@Slf4j
public class AcademicController {

    private final AcademicService academicService;
    private final SubjectRepository subjectRepository; // Repository injected for persistence

    // ==========================================
    // AI STUDY PROTOCOLS
    // ==========================================

    @PostMapping("/study-plan")
    public ResponseEntity<ApiResponse<AcademicPlanPayload>> createPlan(
            @Valid @RequestBody AcademicStudyPlanRequest request) {
        log.info("Processing study plan request for user: {}", request.getUserId());

        AcademicPlanPayload payload = academicService.createStudyPlan(request);

        ApiResponse<AcademicPlanPayload> resp = ApiResponse.of(
                payload.getTodayPlan(),
                IntentType.STUDY_PLANNING,
                payload.getMascotAction() != null ? payload.getMascotAction() : MascotAction.THINKING,
                payload.getEmotion() != null ? payload.getEmotion() : Emotion.HELPFUL,
                "Show full execution timeline terminal",
                VoiceMeta.serious(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/today-plan")
    public ResponseEntity<ApiResponse<AcademicPlanPayload>> todayPlan(@RequestParam Long userId) {
        log.info("Fetching today's plan for user: {}", userId);

        AcademicPlanPayload payload = academicService.getTodayPlan(userId);

        ApiResponse<AcademicPlanPayload> resp = ApiResponse.of(
                payload.getTodayPlan(),
                IntentType.STUDY_PLANNING,
                MascotAction.WAVE,
                Emotion.SUPPORTIVE,
                "Highlight immediate action card",
                VoiceMeta.friendly(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/exam-prep")
    public ResponseEntity<ApiResponse<AcademicPlanPayload>> examPreparation(
            @RequestParam Long userId,
            @RequestParam String subject,
            @RequestParam int daysUntilExam) {
        log.info("Generating exam preparation for subject: {}, days left: {}", subject, daysUntilExam);

        AcademicPlanPayload payload = academicService.getExamPreparationPlan(userId, subject, daysUntilExam);

        ApiResponse<AcademicPlanPayload> resp = ApiResponse.of(
                payload.getTodayPlan(),
                IntentType.EXAM_QUERY,
                payload.getMascotAction() != null ? payload.getMascotAction() : MascotAction.THINKING,
                payload.getEmotion() != null ? payload.getEmotion() : Emotion.SERIOUS,
                "Initiate high-intensity countdown UI",
                VoiceMeta.serious(),
                payload
        );
        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // SUBJECT MANAGEMENT (Neon DB Persistence)
    // ==========================================

    /**
     * Fetch all tracked subjects for a specific user.
     */
    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> getSubjects(@RequestParam Long userId) {
        log.info("Retrieving subject logs for user: {}", userId);
        return ResponseEntity.ok(subjectRepository.findByUserId(userId));
    }

    /**
     * Save or Update a subject in Neon PostgreSQL.
     */
    @PostMapping("/subjects/save")
    public ResponseEntity<Subject> saveSubject(@RequestBody Subject subject) {
        log.info("Syncing subject tracker: {}", subject.getName());
        return ResponseEntity.ok(subjectRepository.save(subject));
    }

    /**
     * Delete a subject from the database.
     */
    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        log.warn("Deleting subject tracker with ID: {}", id);
        subjectRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Academic Command Center is Online");
    }
}