package com.studentlifeos.service;

import com.studentlifeos.dto.DevicePairingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceManagementService {

    private final Map<String, PairedDevice> pairedDevices = new ConcurrentHashMap<>();
    private final Map<String, String> pairingCodes = new ConcurrentHashMap<>();

    public String generatePairingCode(String userId) {
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        pairingCodes.put(code, userId);
        log.info("Generated pairing code {} for user {}", code, userId);
        return code;
    }

    public boolean validatePairingCode(String code, String userId) {
        return userId.equals(pairingCodes.get(code));
    }

    public String pairDevice(DevicePairingRequest request) {
        if (!validatePairingCode(request.getPairingCode(), request.getUserId())) {
            throw new IllegalArgumentException("Invalid pairing code");
        }

        String deviceId = UUID.randomUUID().toString();
        PairedDevice device = PairedDevice.builder()
                .deviceId(deviceId)
                .userId(request.getUserId())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .deviceFingerprint(request.getDeviceFingerprint())
                .pairedAt(System.currentTimeMillis())
                .build();

        pairedDevices.put(deviceId, device);
        pairingCodes.remove(request.getPairingCode());

        log.info("Successfully paired device {} for user {}", deviceId, request.getUserId());
        return deviceId;
    }

    public boolean isDevicePaired(String userId, String deviceId) {
        PairedDevice device = pairedDevices.get(deviceId);
        return device != null && device.getUserId().equals(userId);
    }

    public void unpairDevice(String userId, String deviceId) {
        PairedDevice device = pairedDevices.get(deviceId);
        if (device != null && device.getUserId().equals(userId)) {
            pairedDevices.remove(deviceId);
            log.info("Unpaired device {} for user {}", deviceId, userId);
        }
    }

    public PairedDevice getDevice(String deviceId) {
        return pairedDevices.get(deviceId);
    }

    @lombok.Builder
    @lombok.Data
    public static class PairedDevice {
        private String deviceId;
        private String userId;
        private String deviceName;
        private DevicePairingRequest.DeviceType deviceType;
        private String deviceFingerprint;
        private long pairedAt;
    }
}
