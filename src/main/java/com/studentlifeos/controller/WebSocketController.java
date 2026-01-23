package com.studentlifeos.controller;

import com.studentlifeos.dto.CodingAssistanceRequest;
import com.studentlifeos.dto.CodingAssistanceResponse;
import com.studentlifeos.service.CodingAssistanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final CodingAssistanceService codingAssistanceService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/coding-assistance")
    public void handleCodingAssistance(@Payload @NonNull CodingAssistanceRequest request) {
        log.info("Received WebSocket coding assistance request from device: {}", request.getDeviceId());
        
        try {
            CodingAssistanceResponse response = codingAssistanceService.processAssistanceRequest(request);
            
            // Send response back to specific user's queue
            String destination = "/queue/coding-assistance/" + request.getUserId();
            messagingTemplate.convertAndSend(destination, response);
            
        } catch (Exception e) {
            log.error("Error processing coding assistance request", e);
            
            // Send error response
            CodingAssistanceResponse errorResponse = CodingAssistanceResponse.builder()
                    .sessionId(request.getSessionId())
                    .spokenResponse("I'm having trouble processing your request right now. Please try again.")
                    .build();
                    
            String destination = "/queue/coding-assistance/" + request.getUserId();
            messagingTemplate.convertAndSend(destination, errorResponse);
        }
    }

    @MessageMapping("/hotword-detected")
    public void handleHotwordDetected(@Payload @NonNull HotwordDetectionRequest request) {
        log.info("Hotword '{}' detected on device: {}", request.hotword(), request.deviceId());
        
        // Notify the user's PC that hotword was detected
        String destination = "/topic/hotword-detected/" + request.userId();
        messagingTemplate.convertAndSend(destination, request);
    }

    public record HotwordDetectionRequest(
        String userId,
        String deviceId,
        String hotword,
        long timestamp
    ) {}
}
