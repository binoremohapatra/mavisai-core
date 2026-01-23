package com.studentlifeos.controller;

import com.studentlifeos.dto.AIChatRequest;
import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.service.AIOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIOrchestratorService orchestratorService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<?>> chat(@Valid @RequestBody AIChatRequest request) {
        return ResponseEntity.ok(orchestratorService.handleChat(request));
    }
}




