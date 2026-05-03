package com.microservices.user_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tüm exception'ları tek noktada yakalar ve tutarlı JSON cevabına dönüştürür.
 * SRP: Her handler tek bir exception türünden sorumlu.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Kullanıcı bulunamadı → 404 */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("Kullanıcı bulunamadı: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Kullanıcı zaten mevcut → 409 Conflict */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("Kullanıcı zaten mevcut: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * @Valid annotation hatası → 400.
     * Tüm validation hatalarını tek mesajda toplar.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation hatası: {}", errors);
        return buildError(HttpStatus.BAD_REQUEST, errors);
    }

    /** İş kuralı ihlali → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Geçersiz istek: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Beklenmeyen hatalar → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Beklenmeyen hata: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Sunucu hatası oluştu.");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        // LinkedHashMap ile alan sırası korunur
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

