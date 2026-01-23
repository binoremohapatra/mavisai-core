package com.studentlifeos.dto;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
public class DevicePairingRequest {
    
    @NotBlank
    private String userId;
    
    @NotBlank
    private String deviceId;
    
    @NotBlank
    private String deviceName; // e.g., "iPhone 15", "Samsung Galaxy"
    
    private DeviceType deviceType;
    
    private String pairingCode; // 6-digit pairing code
    
    private String deviceFingerprint; // Unique device identifier
    
    private String publicKey; // For encryption
    
    public enum DeviceType {
        MOBILE_PHONE,
        TABLET,
        LAPTOP,
        DESKTOP,
        WEARABLE
    }
}
