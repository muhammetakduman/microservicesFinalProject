package com.microservices.stok_service.service;

import com.microservices.stok_service.dto.EventPayloads;
import com.microservices.stok_service.dto.StockUpdateRequest;
import com.microservices.stok_service.dto.StockUpdateResponse;
import com.microservices.stok_service.entity.ProductStock;
import com.microservices.stok_service.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stok iş mantığı (Domain Service).
 * Tüm SAGA adımları (reserve / commit / release) ve REST işlemleri burada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockDomainService {

    private final ProductStockRepository productStockRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.stock-reserved:stock.reserved}")
    private String stockReservedRoutingKey;

    @Value("${rabbitmq.routing-key.stock-not-available:stock.not-available}")
    private String stockNotAvailableRoutingKey;

    // -------------------------------------------------------
    // SAGA – OrderCreatedEvent → stok rezerve et
    // -------------------------------------------------------
    @Transactional
    public void reserveStock(EventPayloads.OrderCreatedEvent event) {
        log.info("Stok rezervasyonu başlıyor – orderId: {}", event.getOrderId());

        for (EventPayloads.OrderCreatedEvent.OrderItem item : event.getItems()) {
            ProductStock stock = productStockRepository.findById(item.getProductId()).orElse(null);

            if (stock == null) {
                log.warn("Stok bulunamadı – productId: {}", item.getProductId());
                publishStockNotAvailable(event.getOrderId(), item.getProductId(), "Ürün stokta bulunamadı");
                return;
            }

            try {
                stock.reserve(item.getQuantity());
                productStockRepository.save(stock);
                log.info("Stok rezerve edildi – productId: {}, miktar: {}", item.getProductId(), item.getQuantity());
            } catch (IllegalStateException e) {
                log.warn("Stok yetersiz – productId: {}, sebep: {}", item.getProductId(), e.getMessage());
                rollbackReservations(event);
                publishStockNotAvailable(event.getOrderId(), item.getProductId(), e.getMessage());
                return;
            }
        }

        EventPayloads.StockReservedEvent reservedEvent =
                EventPayloads.StockReservedEvent.builder().orderId(event.getOrderId()).build();
        rabbitTemplate.convertAndSend(exchange, stockReservedRoutingKey, reservedEvent);
        log.info("StockReservedEvent yayınlandı – orderId: {}", event.getOrderId());
    }

    // -------------------------------------------------------
    // SAGA – PaymentSuccessEvent → rezervi commit et
    // -------------------------------------------------------
    @Transactional
    public void commitStock(EventPayloads.PaymentSuccessEvent event) {
        log.info("Stok commit ediliyor – orderId: {}", event.getOrderId());

        for (EventPayloads.PaymentSuccessEvent.OrderItem item : event.getItems()) {
            ProductStock stock = productStockRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Stok bulunamadı – productId: " + item.getProductId()));
            stock.commit(item.getQuantity());
            productStockRepository.save(stock);
            log.info("Stok commit edildi – productId: {}, miktar: {}", item.getProductId(), item.getQuantity());
        }
    }

    // -------------------------------------------------------
    // SAGA – PaymentFailedEvent → rezervi serbest bırak
    // -------------------------------------------------------
    @Transactional
    public void releaseStock(EventPayloads.PaymentFailedEvent event) {
        log.info("Stok rezervasyonu serbest bırakılıyor – orderId: {}", event.getOrderId());

        for (EventPayloads.PaymentFailedEvent.OrderItem item : event.getItems()) {
            productStockRepository.findById(item.getProductId()).ifPresent(stock -> {
                stock.release(item.getQuantity());
                productStockRepository.save(stock);
                log.info("Rezervasyon serbest bırakıldı – productId: {}, miktar: {}",
                        item.getProductId(), item.getQuantity());
            });
        }
    }

    // -------------------------------------------------------
    // REST – Stok sorgula
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public StockUpdateResponse getStock(Long productId) {
        ProductStock stock = productStockRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Stok bulunamadı – productId: " + productId));
        return toResponse(stock, "OK");
    }

    // -------------------------------------------------------
    // REST – Stok güncelle (seller/admin isteği)
    // -------------------------------------------------------
    @Transactional
    public StockUpdateResponse updateStock(StockUpdateRequest request) {
        ProductStock stock = productStockRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Stok bulunamadı – productId: " + request.getProductId()));

        switch (request.getOperation().toUpperCase()) {
            case "INCREASE" -> stock.increase(request.getQuantity());
            case "DECREASE" -> stock.decrease(request.getQuantity());
            default -> throw new IllegalArgumentException("Geçersiz operasyon: " + request.getOperation());
        }
        productStockRepository.save(stock);
        log.info("Stok güncellendi – productId: {}, operasyon: {}, miktar: {}",
                request.getProductId(), request.getOperation(), request.getQuantity());
        return toResponse(stock, "Stok güncellendi");
    }

    // -------------------------------------------------------
    // REST – Stok oluştur (yeni ürün onaylanınca)
    // -------------------------------------------------------
    @Transactional
    public StockUpdateResponse createStock(Long productId, String productName, Long sellerId, Integer quantity) {
        ProductStock stock = ProductStock.builder()
                .productId(productId)
                .productName(productName)
                .sellerId(sellerId)
                .availableQuantity(quantity)
                .reservedQuantity(0)
                .build();
        productStockRepository.save(stock);
        log.info("Yeni stok oluşturuldu – productId: {}, miktar: {}", productId, quantity);
        return toResponse(stock, "Stok oluşturuldu");
    }

    // -------------------------------------------------------
    // Yardımcı metodlar
    // -------------------------------------------------------
    private void rollbackReservations(EventPayloads.OrderCreatedEvent event) {
        for (EventPayloads.OrderCreatedEvent.OrderItem item : event.getItems()) {
            productStockRepository.findById(item.getProductId()).ifPresent(stock -> {
                try {
                    stock.release(item.getQuantity());
                    productStockRepository.save(stock);
                } catch (IllegalStateException ignored) {
                    // Henüz reserve edilmemiş ürün, geç
                }
            });
        }
    }

    private void publishStockNotAvailable(Long orderId, Long productId, String reason) {
        EventPayloads.StockNotAvailableEvent event = EventPayloads.StockNotAvailableEvent.builder()
                .orderId(orderId)
                .productId(productId)
                .reason(reason)
                .build();
        rabbitTemplate.convertAndSend(exchange, stockNotAvailableRoutingKey, event);
    }

    private StockUpdateResponse toResponse(ProductStock stock, String message) {
        return StockUpdateResponse.builder()
                .productId(stock.getProductId())
                .productName(stock.getProductName())
                .sellerId(stock.getSellerId())
                .availableQuantity(stock.getAvailableQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .message(message)
                .build();
    }
}

