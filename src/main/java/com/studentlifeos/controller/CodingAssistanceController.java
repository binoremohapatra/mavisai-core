package com.studentlifeos.controller;

import com.studentlifeos.dto.CodingAssistanceRequest;
import com.studentlifeos.dto.CodingAssistanceResponse;
import com.studentlifeos.dto.DevicePairingRequest;
import com.studentlifeos.service.CodingAssistanceService;
import com.studentlifeos.service.DeviceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coding")
@RequiredArgsConstructor
@Slf4j
public class CodingAssistanceController {

    private final CodingAssistanceService codingAssistanceService;
    private final DeviceManagementService deviceManagementService;

    @PostMapping("/assist")
    public ResponseEntity<CodingAssistanceResponse> getAssistance(@Valid @RequestBody CodingAssistanceRequest request) {
        log.info("Received coding assistance request from device: {}", request.getDeviceId());
        
        CodingAssistanceResponse response = codingAssistanceService.processAssistanceRequest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pair")
    public ResponseEntity<PairingResponse> pairDevice(@Valid @RequestBody DevicePairingRequest request) {
        try {
            String deviceId = deviceManagementService.pairDevice(request);
            return ResponseEntity.ok(new PairingResponse(true, deviceId, "Device paired successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new PairingResponse(false, null, "Invalid pairing code: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error pairing device", e);
            return ResponseEntity.internalServerError()
                    .body(new PairingResponse(false, null, "Failed to pair device"));
        }
    }

    @PostMapping("/pairing-code")
    public ResponseEntity<PairingCodeResponse> generatePairingCode(@RequestParam String userId) {
        String code = deviceManagementService.generatePairingCode(userId);
        return ResponseEntity.ok(new PairingCodeResponse(code));
    }

    @DeleteMapping("/unpair/{deviceId}")
    public ResponseEntity<Void> unpairDevice(@PathVariable String deviceId, @RequestParam String userId) {
        deviceManagementService.unpairDevice(userId, deviceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{deviceId}")
    public ResponseEntity<DeviceStatusResponse> getDeviceStatus(@PathVariable String deviceId, 
                                                           @RequestParam String userId) {
        boolean isPaired = deviceManagementService.isDevicePaired(userId, deviceId);
        var device = isPaired ? deviceManagementService.getDevice(deviceId) : null;
        
        return ResponseEntity.ok(new DeviceStatusResponse(isPaired, device));
    }

    // Response DTOs
    public record PairingResponse(boolean success, String deviceId, String message) {}
    public record PairingCodeResponse(String pairingCode) {}
    public record DeviceStatusResponse(boolean paired, DeviceManagementService.PairedDevice device) {}
}
