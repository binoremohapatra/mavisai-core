package com.studentlifeos.controller;

import com.studentlifeos.dto.TtsRequest;
import com.studentlifeos.service.PythonAIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
@Slf4j
public class TtsController {

    private final PythonAIService pythonAIService;

    @PostMapping("/speak")
    public ResponseEntity<?> speak(@Valid @RequestBody TtsRequest request) {
        log.info("TTS Request received: {} chars", request.getText().length());
        
        // Call Python Backend for Audio
        byte[] audioData = pythonAIService.generateAudio(request.getText());
        
        if (audioData != null && audioData.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioData.length);
            
            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Voice service unavailable"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean isHealthy = pythonAIService.isHealthy();
        return ResponseEntity.ok(Map.of("status", isHealthy ? "UP" : "DOWN"));
    }
}
