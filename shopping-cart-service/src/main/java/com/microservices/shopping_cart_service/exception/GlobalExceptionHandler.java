package com.microservices.shopping_cart_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uygulama genelinde fırlatılan exception'ları yakalar ve
 * tutarlı bir JSON hata cevabına dönüştürür.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Sepet bulunamadı → 404 */
    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartNotFound(CartNotFoundException ex) {
        log.warn("Sepet bulunamadı: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Kalem bulunamadı → 404 */
    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleItemNotFound(CartItemNotFoundException ex) {
        log.warn("Kalem bulunamadı: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** İş kuralı ihlali (örn. miktar <= 0) → 400 */
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
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }
}

