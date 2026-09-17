package com.ussdauto.api.web.advice;

import com.ussdauto.api.exception.DeviceAuthMismatchException;
import com.ussdauto.api.exception.DeviceNotFoundException;
import com.ussdauto.api.exception.InvalidStatusTransitionException;
import com.ussdauto.api.exception.InvalidTransactionStateException;
import com.ussdauto.api.exception.NoDeviceAvailableException;
import com.ussdauto.api.exception.SimSlotNotFoundException;
import com.ussdauto.api.exception.TransactionNotFoundException;
import com.ussdauto.api.exception.UssdTemplateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "Validation échouée", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "Corps de la requête manquant ou invalide", null));
    }

    @ExceptionHandler({TransactionNotFoundException.class, DeviceNotFoundException.class, SimSlotNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage(), null));
    }

    @ExceptionHandler(NoDeviceAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleNoDevice(NoDeviceAvailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null));
    }

    @ExceptionHandler(UssdTemplateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoTemplate(UssdTemplateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidStatusTransitionException ex) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidTransactionStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(InvalidTransactionStateException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(HttpStatus.FORBIDDEN, ex.getMessage(), null));
    }

    @ExceptionHandler(DeviceAuthMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleDeviceAuthMismatch(DeviceAuthMismatchException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(HttpStatus.FORBIDDEN, ex.getMessage(), null));
    }

    private Map<String, Object> body(HttpStatus status, String message, Object details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return body;
    }
}
