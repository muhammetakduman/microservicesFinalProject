package com.microservices.order_service.entity;

/**
 * Sipariş durum makinesi.
 *
 * SAGA akışı:
 *  PENDING → (stok rezerve edildi) → STOCK_RESERVED
 *          → (ödeme başarılı)       → CONFIRMED
 *          → (stok yetersiz)        → FAILED
 *          → (ödeme başarısız)      → FAILED
 *          → (müşteri iptal)        → CANCELLED
 */
public enum OrderStatus {

    /** Sipariş oluşturuldu, stok rezervasyonu bekleniyor */
    PENDING,

    /** Stok rezerve edildi, ödeme bekleniyor */
    STOCK_RESERVED,

    /** Ödeme başarılı — sipariş onaylandı */
    CONFIRMED,

    /** Stok yetersiz veya ödeme başarısız — sipariş başarısız */
    FAILED,

    /** Müşteri tarafından iptal edildi */
    CANCELLED,

    /** Kargoya verildi */
    SHIPPED,

    /** Teslim edildi */
    DELIVERED
}

