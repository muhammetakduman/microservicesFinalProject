package com.microservices.auth_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Auth servisine özgü hata yönetimi.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Yanlış şifre / kullanıcı bulunamadı → 401 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        // Güvenlik: e-posta mi şifre mi yanlış bilgisi VERİLMEZ
        log.warn("Başarısız giriş denemesi");
        return buildError(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı.");
    }

    /** Hesap devre dışı → 403 */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        log.warn("Devre dışı hesaba giriş denemesi");
        return buildError(HttpStatus.FORBIDDEN, "Hesabınız devre dışı bırakılmıştır.");
    }

    /** Auth iş kuralı hatası → 401 */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthException ex) {
        log.warn("Auth hatası: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** E-posta zaten kayıtlı → 409 */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Kayıt çakışması: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** @Valid hatası → 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation hatası: {}", errors);
        return buildError(HttpStatus.BAD_REQUEST, errors);
    }

    /** Beklenmeyen hata → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Beklenmeyen hata: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Sunucu hatası oluştu.");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

