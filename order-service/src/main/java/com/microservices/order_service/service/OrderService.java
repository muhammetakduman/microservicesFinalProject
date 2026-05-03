package com.microservices.order_service.service;

import com.microservices.order_service.dto.CreateOrderRequest;
import com.microservices.order_service.dto.OrderResponse;
import com.microservices.order_service.entity.Order;
import com.microservices.order_service.entity.OrderItem;
import com.microservices.order_service.entity.OrderStatus;
import com.microservices.order_service.event.OrderCreatedEvent;
import com.microservices.order_service.repository.OrderItemRepository;
import com.microservices.order_service.repository.OrderRepository;
import com.microservices.order_service.saga.PaymentCardStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order iş mantığı servisi.
 *
 * Sorumluluklar:
 *  - Sipariş oluşturma ve SAGA başlatma
 *  - Müşteri / satıcı / admin sipariş sorgulama
 *  - Sipariş iptal etme
 *  - Satıcı kalem durumu güncelleme
 *  - Admin sipariş durumu güncelleme
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentCardStore paymentCardStore;

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.order-created:order.created}")
    private String orderCreatedRoutingKey;

    // ========================
    // MÜŞTERİ — Sipariş Oluştur
    // ========================

    /**
     * Yeni sipariş oluşturur ve SAGA sürecini başlatır.
     *
     * Adımlar:
     *  1. Order + OrderItem'ları kaydet (PENDING)
     *  2. Kart bilgisini PaymentCardStore'a al (geçici, DB'ye kaydedilmez)
     *  3. OrderCreatedEvent → order.created.queue (stok-service dinler)
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Sipariş oluşturuluyor – customerId: {}", request.getCustomerId());

        // ---- 1. Order entity oluştur ----
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .shippingDetails(request.getShippingDetails())
                .totalAmount(BigDecimal.ZERO) // Kalemler eklendikten sonra hesaplanacak
                .build();

        // ---- 2. Kalemleri ekle ----
        List<OrderItem> items = request.getItems().stream().map(dto -> {
            BigDecimal totalPrice = dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
            return OrderItem.builder()
                    .order(order)
                    .productId(dto.getProductId())
                    .productName(dto.getProductName())
                    .sellerId(dto.getSellerId())
                    .unitPrice(dto.getUnitPrice())
                    .quantity(dto.getQuantity())
                    .totalPrice(totalPrice)
                    .itemStatus(OrderStatus.PENDING)
                    .build();
        }).toList();

        order.setItems(items);

        // ---- 3. Toplam tutarı hesapla ----
        BigDecimal total = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        // ---- 4. Kaydet ----
        orderRepository.save(order);
        log.info("Sipariş kaydedildi – orderId: {}, tutar: {}", order.getId(), total);

        // ---- 5. Kart bilgisini geçici depola (DB'ye gitMEZ) ----
        if (request.getPaymentInfo() != null) {
            request.getPaymentInfo().setOrderId(order.getId());
            request.getPaymentInfo().setCustomerId(request.getCustomerId());
            request.getPaymentInfo().setCustomerEmail(request.getCustomerEmail());
            // Items bilgisini de kaydet — payment-service commit/release için ihtiyaç duyar
            List<com.microservices.order_service.dto.payment.PaymentRequest.OrderItem> paymentItems =
                    order.getItems().stream()
                            .map(i -> new com.microservices.order_service.dto.payment.PaymentRequest.OrderItem(
                                    i.getProductId(), i.getQuantity()))
                            .toList();
            request.getPaymentInfo().setItems(paymentItems);
            paymentCardStore.save(order.getId(), request.getPaymentInfo());
        }

        // ---- 6. OrderCreatedEvent publish et → SAGA başlar ----
        OrderCreatedEvent event = buildOrderCreatedEvent(order);
        rabbitTemplate.convertAndSend(exchange, orderCreatedRoutingKey, event);
        log.info("OrderCreatedEvent publish edildi – orderId: {}", order.getId());

        return toResponse(order);
    }

    // ========================
    // MÜŞTERİ — Siparişlerim
    // ========================

    /**
     * Müşterinin tüm siparişlerini getirir.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Tek sipariş detayı — müşteri kendi siparişini görebilir.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    /**
     * Müşteri siparişini iptal eder.
     * Yalnızca PENDING durumundaki siparişler iptal edilebilir.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerId) {
        Order order = findOrder(orderId);

        // Güvenlik: Siparişin bu müşteriye ait olduğunu doğrula
        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Bu sipariş size ait değil – orderId: " + orderId);
        }

        order.cancel();
        orderRepository.save(order);
        log.info("Sipariş iptal edildi – orderId: {}, customerId: {}", orderId, customerId);

        // Stok reserve edilmişse serbest bırak
        // TODO: stok-service'e StockReleaseEvent gönder
        paymentCardStore.remove(orderId);

        return toResponse(order);
    }

    // ========================
    // SATICI — Satışlarım
    // ========================

    /**
     * Satıcının ürünlerini içeren tüm sipariş kalemlerini döner.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse.OrderItemResponse> getSellerItems(Long sellerId) {
        return orderItemRepository.findBySellerId(sellerId)
                .stream().map(this::toItemResponse).toList();
    }

    /**
     * Satıcı bir sipariş kaleminin durumunu günceller.
     * Örn: PENDING → PREPARING → SHIPPED
     */
    @Transactional
    public OrderResponse.OrderItemResponse updateItemStatus(Long orderItemId, String newStatus, Long sellerId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş kalemi bulunamadı – id: " + orderItemId));

        // Güvenlik: Kalemin bu satıcıya ait olduğunu doğrula
        if (!item.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Bu kalem size ait değil – itemId: " + orderItemId);
        }

        item.setItemStatus(OrderStatus.valueOf(newStatus.toUpperCase()));
        orderItemRepository.save(item);
        log.info("Kalem durumu güncellendi – itemId: {}, yeniDurum: {}", orderItemId, newStatus);

        return toItemResponse(item);
    }

    // ========================
    // ADMİN — Tüm siparişler
    // ========================

    /**
     * Admin: tüm siparişleri listeler.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    /**
     * Admin: tek siparişin detayını getirir.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdForAdmin(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    /**
     * Admin: sipariş durumunu zorla günceller (örn. SHIPPED, DELIVERED).
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
        Order order = findOrder(orderId);
        order.setStatus(OrderStatus.valueOf(newStatus.toUpperCase()));
        orderRepository.save(order);
        log.info("Admin sipariş durumu güncellendi – orderId: {}, yeniDurum: {}", orderId, newStatus);
        return toResponse(order);
    }

    // ========================
    // Yardımcı metodlar
    // ========================

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı – orderId: " + orderId));
    }

    /** Order entity → RabbitMQ event dönüşümü */
    private OrderCreatedEvent buildOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItem> eventItems = order.getItems().stream()
                .map(item -> new OrderCreatedEvent.OrderItem(item.getProductId(), item.getQuantity()))
                .toList();

        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .items(eventItems)
                .build();
    }

    /** Order entity → response DTO dönüşümü */
    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toItemResponse).toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingDetails(order.getShippingDetails())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /** OrderItem entity → item response DTO dönüşümü */
    private OrderResponse.OrderItemResponse toItemResponse(OrderItem item) {
        return OrderResponse.OrderItemResponse.builder()
                .itemId(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sellerId(item.getSellerId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .itemStatus(item.getItemStatus())
                .build();
    }
}

