package com.studentlifeos.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Voice metadata for frontend TTS.
 * Backend NEVER generates audio; frontend may speak replyText based on this.
 */
@Data
@Builder
public class VoiceMeta {
    private boolean speakable;      // should frontend speak replyText?
    private String tone;            // calm | friendly | serious | celebratory
    private double speechRate;      // recommended 0.8 - 1.1
    private boolean interruptible;  // can user interrupt speech?

    public static VoiceMeta calm() {
        return VoiceMeta.builder()
                .speakable(true)
                .tone("calm")
                .speechRate(0.95)
                .interruptible(true)
                .build();
    }

    public static VoiceMeta friendly() {
        return VoiceMeta.builder()
                .speakable(true)
                .tone("friendly")
                .speechRate(1.0)
                .interruptible(true)
                .build();
    }

    public static VoiceMeta serious() {
        return VoiceMeta.builder()
                .speakable(true)
                .tone("serious")
                .speechRate(0.9)
                .interruptible(true)
                .build();
    }

    public static VoiceMeta celebratory() {
        return VoiceMeta.builder()
                .speakable(true)
                .tone("celebratory")
                .speechRate(1.05)
                .interruptible(true)
                .build();
    }

    public static VoiceMeta helpful() {
        return VoiceMeta.builder()
                .speakable(true)
                .tone("friendly")
                .speechRate(0.95)
                .interruptible(true)
                .build();
    }
}




