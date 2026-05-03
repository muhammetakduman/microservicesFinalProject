package com.microservices.payment_service.entity;

/**
 * Ödeme durumu.
 * PENDING  → iyzico'dan cevap bekleniyor
 * SUCCESS  → iyzico onayladı
 * FAILED   → iyzico reddetti veya hata
 * REFUNDED → İade tamamlandı
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}

