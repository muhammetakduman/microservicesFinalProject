package com.microservices.order_service.saga;

import com.microservices.order_service.dto.payment.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SAGA Kart Deposu — Geçici in-memory store.
 *
 * Problem:
 *   Müşteri sipariş oluştururken kart bilgisini girer.
 *   Ödeme, ancak stok rezervasyonu başarılı olduktan SONRA başlar.
 *   Bu iki adım arasında asenkron mesajlaşma vardır (RabbitMQ).
 *   Bu süre zarfında kart bilgisini bir yerde tutmak gerekir.
 *
 * Çözüm:
 *   PaymentCardStore, orderId → PaymentRequest eşlemesini hafızada tutar.
 *   StockReservedEvent geldiğinde kart bilgisi buradan çekilip payment-service'e gönderilir.
 *   Ödeme tamamlandıktan sonra kayıt silinir.
 *
 * ÜRETİM NOTU:
 *   In-memory store yalnızca tek node'da çalışır.
 *   Ölçeklendirme gerektiğinde Redis veya order tablosunda şifrelenmiş sütun kullanın.
 */
@Component
@Slf4j
public class PaymentCardStore {

    /**
     * Thread-safe map: orderId → kart bilgisi
     * ConcurrentHashMap — birden fazla thread'in aynı anda yazmasına izin verir
     */
    private final ConcurrentHashMap<Long, PaymentRequest> store = new ConcurrentHashMap<>();

    /**
     * Kart bilgisini depola.
     * @param orderId    Siparişin ID'si
     * @param request    Kart ve ödeme bilgileri
     */
    public void save(Long orderId, PaymentRequest request) {
        store.put(orderId, request);
        log.debug("Kart bilgisi kaydedildi – orderId: {}", orderId);
    }

    /**
     * Kart bilgisini getir.
     * @param orderId Siparişin ID'si
     * @return Kart bilgisi varsa Optional içinde, yoksa empty
     */
    public Optional<PaymentRequest> get(Long orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    /**
     * Kart bilgisini sil.
     * Ödeme tamamlandıktan (başarılı veya başarısız) sonra çağrılmalı.
     * @param orderId Siparişin ID'si
     */
    public void remove(Long orderId) {
        store.remove(orderId);
        log.debug("Kart bilgisi silindi – orderId: {}", orderId);
    }
}

