package com.studentlifeos.exception;

import com.studentlifeos.dto.ApiResponse;
import com.studentlifeos.dto.VoiceMeta;
import com.studentlifeos.enums.Emotion;
import com.studentlifeos.enums.IntentType;
import com.studentlifeos.enums.MascotAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central error handling.
 * Error responses still follow ApiResponse contract to keep UX consistent.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ApiResponse<Void> body = ApiResponse.of(
                msg,
                IntentType.UNKNOWN,
                MascotAction.ERROR_STATE,
                Emotion.SERIOUS,
                "Show a gentle error state near the form, without submitting anything automatically",
                VoiceMeta.serious(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        ApiResponse<Void> body = ApiResponse.of(
                "Something went wrong. Please try again.",
                IntentType.UNKNOWN,
                MascotAction.ERROR_STATE,
                Emotion.SERIOUS,
                "Display a non-intrusive error state; avoid auto-retrying or navigation",
                VoiceMeta.serious(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}


