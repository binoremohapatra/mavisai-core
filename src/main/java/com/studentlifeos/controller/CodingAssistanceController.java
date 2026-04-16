package com.studentlifeos.controller;

import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.AiBrainRequest;
import com.studentlifeos.dto.AiBrainResponse;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import com.studentlifeos.service.AIClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/coding")
@RequiredArgsConstructor
@Slf4j
public class CodingAssistanceController {

    private final AIClientService aiClientService;

    @PostMapping("/assist")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAssistance(@RequestBody Map<String, Object> request) {
        // Extracting mandatory fields from Axios payload
        String codeSnippet = (String) request.get("codeContext");
        if (codeSnippet == null) codeSnippet = (String) request.get("code");
        
        // Fix: Extracting 'language' to prevent the Non-Null error
        String lang = (String) request.get("language");
        if (lang == null) lang = "auto-detect"; 

        log.info("Neural Fix Protocol: Initiating analysis for language: {}", lang);

        try {
            // Updated Builder with mandatory 'language' field
            AiBrainRequest aiRequest = AiBrainRequest.builder()
                    .intent("CODING_ASSIST")
                    .language(lang) // Added this to fix the 500 error
                    .userMessage("Analyze and fix this code. Return explanation and fixed code block. \n" + codeSnippet)
                    .facts(new HashMap<>(Map.of("IDE", "Mavis_Local")))
                    .desiredTone("technical")
                    .build();

            AiBrainResponse aiResponse = aiClientService.generate(aiRequest);
            String reply = aiResponse.getReplyText() != null ? aiResponse.getReplyText() : "Diagnostics complete.";

            Map<String, String> data = new HashMap<>();
            data.put("explanation", reply);
            data.put("fixedCode", extractCode(reply));

            return ResponseEntity.ok(ApiResponse.of(
                reply,
                IntentType.CODING_ASSIST,
                MascotAction.THINKING,
                Emotion.SUPPORTIVE,
                "Show code diff UI",
                VoiceMeta.friendly(),
                data
            ));

        } catch (Exception e) {
            log.error("CODING_CONTROLLER_CRASH: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.of(
                "Neural Fix Protocol Failed: " + e.getMessage(), 
                IntentType.UNKNOWN, MascotAction.ERROR_STATE, Emotion.SERIOUS, 
                "Error", VoiceMeta.serious(), null
            ));
        }
    }

    private String extractCode(String text) {
        if (text != null && text.contains("```")) {
            String[] parts = text.split("```");
            if (parts.length > 1) {
                return parts[1].replaceAll("^[a-zA-Z]+\\n", "");
            }
        }
        return "";
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Coding Node Online");
    }
}