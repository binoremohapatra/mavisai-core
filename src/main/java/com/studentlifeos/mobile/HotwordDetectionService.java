package com.studentlifeos.mobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for detecting hotwords to activate coding assistance.
 * Uses Android's SpeechRecognizer for continuous listening.
 */
public class HotwordDetectionService implements RecognitionListener {
    
    private static final String TAG = "HotwordDetection";
    private static final List<String> HOTWORDS = List.of(
        "Hey Student",
        "Code Assistant", 
        "Student Life OS",
        "Help me code"
    );
    
    private final Context context;
    private final HotwordListener listener;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    
    public interface HotwordListener {
        void onHotwordDetected(String hotword);
        void onError(String error);
    }
    
    public HotwordDetectionService(Context context, HotwordListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    public void startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening for hotwords");
            return;
        }
        
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(this);
            
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                          RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, 
                          context.getPackageName());
            
            speechRecognizer.startListening(intent);
            isListening = true;
            Log.i(TAG, "Started listening for hotwords");
        } else {
            Log.e(TAG, "Speech recognition not available");
            listener.onError("Speech recognition not available on this device");
        }
    }
    
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
            isListening = false;
            Log.i(TAG, "Stopped listening for hotwords");
        }
    }
    
    private boolean containsHotword(String text) {
        String lowerText = text.toLowerCase();
        for (String hotword : HOTWORDS) {
            if (lowerText.contains(hotword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private String extractHotword(String text) {
        String lowerText = text.toLowerCase();
        for (String hotword : HOTWORDS) {
            if (lowerText.contains(hotword.toLowerCase())) {
                return hotword;
            }
        }
        return null;
    }
    
    // RecognitionListener implementation
    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "Ready for speech");
    }
    
    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech");
    }
    
    @Override
    public void onRmsChanged(float rmsdB) {
        // Audio level changed - could be used for visual feedback
    }
    
    @Override
    public void onBufferReceived(byte[] buffer) {
        // Audio buffer received
    }
    
    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "End of speech");
    }
    
    @Override
    public void onError(int error) {
        String errorMessage = getErrorMessage(error);
        Log.e(TAG, "Speech recognition error: " + errorMessage);
        listener.onError(errorMessage);
        
        // Restart listening after error
        if (isListening) {
            stopListening();
            startListening();
        }
    }
    
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(
            SpeechRecognizer.RESULTS_RECOGNITION);
        
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);
            Log.d(TAG, "Recognized: " + text);
            
            if (containsHotword(text)) {
                String hotword = extractHotword(text);
                Log.i(TAG, "Hotword detected: " + hotword);
                listener.onHotwordDetected(hotword);
            }
        }
        
        // Continue listening
        if (isListening) {
            speechRecognizer.startListening(createListeningIntent());
        }
    }
    
    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(
            SpeechRecognizer.RESULTS_RECOGNITION);
        
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);
            Log.v(TAG, "Partial: " + text);
            
            // Early detection of hotwords in partial results
            if (containsHotword(text)) {
                String hotword = extractHotword(text);
                Log.i(TAG, "Hotword detected in partial: " + hotword);
                listener.onHotwordDetected(hotword);
            }
        }
    }
    
    @Override
    public void onEvent(int eventType, Bundle params) {
        // Other events
    }
    
    private Intent createListeningIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                      RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, 
                      context.getPackageName());
        return intent;
    }
    
    private String getErrorMessage(int error) {
        return switch (error) {
            case SpeechRecognizer.ERROR_AUDIO -> "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT -> "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK -> "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH -> "No match found";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy";
            case SpeechRecognizer.ERROR_SERVER -> "Error from server";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input";
            default -> "Unknown error";
        };
    }
}
