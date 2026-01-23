package com.studentlifeos.mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Main activity for coding assistance on mobile device.
 * Handles hotword detection, voice commands, and WebSocket communication.
 */
public class CodingAssistanceActivity extends AppCompatActivity implements 
        HotwordDetectionService.HotwordListener, TextToSpeech.OnInitListener {
    
    private static final String TAG = "CodingAssistance";
    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    
    // Required permissions
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    
    private HotwordDetectionService hotwordDetection;
    private SpeechRecognizer voiceCommandRecognizer;
    private TextToSpeech textToSpeech;
    private WebSocketClient webSocketClient;
    
    private TextView statusText;
    private TextView responseText;
    private Button pairDeviceButton;
    private Button startListeningButton;
    
    private String userId = "student123"; // Should come from authentication
    private String deviceId = "mobile_device_001";
    private String sessionId = "session_" + System.currentTimeMillis();
    private boolean isPaired = false;
    private boolean isListeningForCommands = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coding_assistance);
        
        initializeViews();
        initializeServices();
        checkPermissions();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        responseText = findViewById(R.id.responseText);
        pairDeviceButton = findViewById(R.id.pairDeviceButton);
        startListeningButton = findViewById(R.id.startListeningButton);
        
        pairDeviceButton.setOnClickListener(v -> initiatePairing());
        startListeningButton.setOnClickListener(v -> toggleCommandListening());
    }
    
    private void initializeServices() {
        // Initialize hotword detection
        hotwordDetection = new HotwordDetectionService(this, this);
        
        // Initialize voice command recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceCommandRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            voiceCommandRecognizer.setRecognitionListener(new VoiceCommandListener());
        }
        
        // Initialize text-to-speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Initialize WebSocket connection
        initializeWebSocket();
    }
    
    private void checkPermissions() {
        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        } else {
            startHotwordDetection();
        }
    }
    
    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                       @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (hasAllPermissions()) {
                startHotwordDetection();
            } else {
                Toast.makeText(this, "Permissions required for coding assistance", 
                             Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void startHotwordDetection() {
        hotwordDetection.startListening();
        statusText.setText("Listening for hotwords... Say 'Hey Student' or 'Code Assistant'");
    }
    
    private void initializeWebSocket() {
        try {
            URI serverUri = new URI("ws://localhost:8080/ws/coding-assistance");
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.i(TAG, "WebSocket connected");
                    runOnUiThread(() -> statusText.setText("Connected to server"));
                }
                
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "WebSocket message: " + message);
                    handleServerMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.i(TAG, "WebSocket closed: " + reason);
                    runOnUiThread(() -> statusText.setText("Disconnected from server"));
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                    runOnUiThread(() -> statusText.setText("Connection error"));
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing WebSocket", e);
        }
    }
    
    // HotwordListener implementation
    @Override
    public void onHotwordDetected(String hotword) {
        Log.i(TAG, "Hotword detected: " + hotword);
        runOnUiThread(() -> {
            statusText.setText("Hotword detected! Listening for command...");
            startVoiceCommandListening();
        });
        
        // Notify PC via WebSocket
        sendHotwordNotification(hotword);
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Hotword detection error: " + error);
        runOnUiThread(() -> statusText.setText("Error: " + error));
    }
    
    private void startVoiceCommandListening() {
        if (voiceCommandRecognizer != null && !isListeningForCommands) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                          RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What coding help do you need?");
            
            voiceCommandRecognizer.startListening(intent);
            isListeningForCommands = true;
        }
    }
    
    private void stopVoiceCommandListening() {
        if (voiceCommandRecognizer != null && isListeningForCommands) {
            voiceCommandRecognizer.stopListening();
            isListeningForCommands = false;
        }
    }
    
    private void sendHotwordNotification(String hotword) {
        try {
            JSONObject notification = new JSONObject();
            notification.put("userId", userId);
            notification.put("deviceId", deviceId);
            notification.put("hotword", hotword);
            notification.put("timestamp", System.currentTimeMillis());
            
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(notification.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending hotword notification", e);
        }
    }
    
    private void sendCodingAssistanceRequest(String voiceCommand) {
        try {
            JSONObject request = new JSONObject();
            request.put("userId", userId);
            request.put("deviceId", deviceId);
            request.put("sessionId", sessionId);
            request.put("voiceCommand", voiceCommand);
            request.put("assistanceType", "CODE_REVIEW");
            
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(request.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending assistance request", e);
        }
    }
    
    private void handleServerMessage(String message) {
        try {
            JSONObject response = new JSONObject(message);
            String spokenResponse = response.optString("spokenResponse", "");
            String codeSuggestion = response.optString("codeSuggestion", "");
            
            runOnUiThread(() -> {
                responseText.setText(spokenResponse);
                if (!codeSuggestion.isEmpty()) {
                    responseText.append("\n\nCode:\n" + codeSuggestion);
                }
            });
            
            // Speak the response
            if (!spokenResponse.isEmpty() && textToSpeech != null) {
                textToSpeech.speak(spokenResponse, TextToSpeech.QUEUE_FLUSH, null, null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing server message", e);
        }
    }
    
    private void initiatePairing() {
        // This would typically involve showing a pairing code UI
        // For now, simulate successful pairing
        isPaired = true;
        pairDeviceButton.setText("Device Paired");
        pairDeviceButton.setEnabled(false);
        Toast.makeText(this, "Device paired successfully", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleCommandListening() {
        if (isListeningForCommands) {
            stopVoiceCommandListening();
            startListeningButton.setText("Start Listening");
        } else {
            startVoiceCommandListening();
            startListeningButton.setText("Stop Listening");
        }
    }
    
    // TextToSpeech.OnInitListener implementation
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported for TTS");
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
        }
    }
    
    private class VoiceCommandListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for voice command");
        }
        
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of voice command");
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {}
        
        @Override
        public void onBufferReceived(byte[] buffer) {}
        
        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "End of voice command");
        }
        
        @Override
        public void onError(int error) {
            Log.e(TAG, "Voice command error: " + error);
            runOnUiThread(() -> statusText.setText("Command recognition error"));
        }
        
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (matches != null && !matches.isEmpty()) {
                String command = matches.get(0);
                Log.i(TAG, "Voice command: " + command);
                
                runOnUiThread(() -> {
                    statusText.setText("Command: " + command);
                    sendCodingAssistanceRequest(command);
                });
            }
            
            isListeningForCommands = false;
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {}
        
        @Override
        public void onEvent(int eventType, Bundle params) {}
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (hotwordDetection != null) {
            hotwordDetection.stopListening();
        }
        
        if (voiceCommandRecognizer != null) {
            voiceCommandRecognizer.destroy();
        }
        
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
}
