package com.studentlifeos.enums;

/**
 * Visual-only actions for the mascot.
 * Maps directly to frontend FBX animation files.
 * IMPORTANT: These are guidance cues only, never real UI actions.
 */
public enum MascotAction {
    // Base States
    IDLE,
    THINKING,
    SPEAKING,
    BREATHING,
    BREATHING_ANIMATION, // Alias for BREATHING

    // Standard Gestures
    WAVE_HELLO,
    POINT_TO_BUTTON,
    CELEBRATE,      // Maps to victory.fbx
    ERROR_STATE,    // Maps to sadidle.fbx
    SERIOUS_WARNING,// Maps to angrypoint.fbx

    // ✅ NEW ANIMATIONS (From your screenshot)
    WAVE,           // wave.fbx
    VICTORY,        // victory.fbx
    SAD,            // sadidle.fbx
    ANGRY,          // angrypoint.fbx
    DEFEAT,         // defeat.fbx
    THANKFUL,       // thankful.fbx
    WALKING         // walking.fbx
}


