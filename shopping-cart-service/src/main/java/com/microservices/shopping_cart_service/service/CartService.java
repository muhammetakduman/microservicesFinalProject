package com.microservices.shopping_cart_service.service;

import com.microservices.shopping_cart_service.dto.AddToCartRequest;
import com.microservices.shopping_cart_service.dto.CartResponse;
import com.microservices.shopping_cart_service.dto.UpdateCartItemRequest;
import com.microservices.shopping_cart_service.entity.CartItem;
import com.microservices.shopping_cart_service.entity.ShoppingCart;
import com.microservices.shopping_cart_service.exception.CartItemNotFoundException;
import com.microservices.shopping_cart_service.exception.CartNotFoundException;
import com.microservices.shopping_cart_service.repository.CartItemRepository;
import com.microservices.shopping_cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sepet servisinin tüm iş mantığını barındırır.
 *
 * SOLID uyumu:
 *  - SRP : Yalnızca sepet işlemlerinden sorumlu.
 *  - OCP : Yeni sepet operasyonu eklemek mevcut metotları değiştirmeyi gerektirmez.
 *  - DIP : Repository interface'lerine bağımlı, concrete class'lara değil.
 *
 * Rich Domain Model:
 *  addItem / removeItem / clear operasyonları ShoppingCart entity içindedir.
 *  Service, entity metodlarını çağırır; iş kurallarını entity yönetir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    // ========================
    // Sepeti Görüntüle
    // ========================

    /**
     * Müşterinin mevcut sepetini getirir.
     * Sepet yoksa otomatik olarak boş bir sepet oluşturulur (lazy init).
     *
     * @param customerId Müşteri ID'si
     * @return Sepetin tüm içeriği
     */
    @Transactional
    public CartResponse getCart(Long customerId) {
        // Sepet yoksa otomatik oluştur — müşteri ilk açılışında boş sepet görür
        ShoppingCart cart = getOrCreateCart(customerId);
        log.info("Sepet getirildi – customerId: {}, kalem sayısı: {}", customerId, cart.getItems().size());
        return toResponse(cart);
    }

    // ========================
    // Ürün Ekle
    // ========================

    /**
     * Sepete ürün ekler.
     * Aynı ürün zaten sepette varsa miktarı artırılır (entity içindeki addItem mantığı).
     *
     * @param customerId Müşteri ID'si
     * @param request    Eklenecek ürün bilgileri
     * @return Güncel sepet
     */
    @Transactional
    public CartResponse addItem(Long customerId, AddToCartRequest request) {
        // Gerekli alan validasyonu
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Miktar en az 1 olmalıdır.");
        }

        ShoppingCart cart = getOrCreateCart(customerId);

        // Yeni kalem oluştur — snapshot alanları request'ten alınır
        CartItem newItem = CartItem.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .sellerId(request.getSellerId())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .build();

        // Ekleme mantığı entity'de: aynı ürünse miktar artar, yoksa yeni kalem oluşur
        cart.addItem(newItem);
        cartRepository.save(cart);

        log.info("Ürün eklendi – customerId: {}, productId: {}, miktar: {}",
                customerId, request.getProductId(), request.getQuantity());

        return toResponse(cart);
    }

    // ========================
    // Kalem Miktarını Güncelle
    // ========================

    /**
     * Sepetteki bir kalemin miktarını günceller.
     *
     * @param itemId  Güncellenecek kalemin ID'si
     * @param request Yeni miktar
     * @return Güncel sepet
     */
    @Transactional
    public CartResponse updateItem(Long customerId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        // Güvenlik: Kalemin bu müşteriye ait olduğunu kontrol et
        if (!item.getCart().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Bu kalem size ait değil — itemId: " + itemId);
        }

        // Miktar validasyonu entity içinde yapılır (Rich Domain Model)
        item.updateQuantity(request.getQuantity());
        cartItemRepository.save(item);

        log.info("Kalem güncellendi – itemId: {}, yeniMiktar: {}", itemId, request.getQuantity());

        return toResponse(item.getCart());
    }

    // ========================
    // Kalem Sil
    // ========================

    /**
     * Sepetten belirli bir ürünü kaldırır.
     *
     * @param customerId Müşteri ID'si
     * @param itemId     Silinecek kalemin ID'si
     * @return Güncel sepet
     */
    @Transactional
    public CartResponse removeItem(Long customerId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        // Güvenlik: Kalemin bu müşteriye ait olduğunu kontrol et
        if (!item.getCart().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Bu kalem size ait değil — itemId: " + itemId);
        }

        ShoppingCart cart = item.getCart();

        // entity metoduyla kaldır → orphanRemoval devreye girer
        cart.removeItem(item.getProductId());
        cartRepository.save(cart);

        log.info("Kalem silindi – customerId: {}, itemId: {}", customerId, itemId);

        return toResponse(cart);
    }

    // ========================
    // Sepeti Temizle
    // ========================

    /**
     * Müşterinin sepetini tamamen boşaltır.
     *
     * İki senaryo:
     *  1. Müşteri manuel olarak "Sepeti Temizle" butonuna basar (REST endpoint).
     *  2. Sipariş onaylandı → cart.clear.queue → CartEventListener bu metodu çağırır.
     *
     * @param customerId Müşteri ID'si
     */
    @Transactional
    public void clearCart(Long customerId) {
        // Sepet yoksa hata fırlatma — idempotent davran (zaten temizse sorun yok)
        cartRepository.findByCustomerId(customerId).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
            log.info("Sepet temizlendi – customerId: {}", customerId);
        });
    }

    // ========================
    // Yardımcı metodlar
    // ========================

    /**
     * Müşterinin sepetini getirir, yoksa yeni boş sepet oluşturur.
     * "Get or Create" pattern — lazy initialization.
     *
     * @param customerId Müşteri ID'si
     * @return Mevcut ya da yeni oluşturulan sepet
     */
    private ShoppingCart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    log.info("Yeni sepet oluşturuluyor – customerId: {}", customerId);
                    ShoppingCart newCart = ShoppingCart.builder()
                            .customerId(customerId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * ShoppingCart entity → response DTO dönüşümü.
     * Mapping mantığı tek yerde (DRY prensibi).
     */
    private CartResponse toResponse(ShoppingCart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartResponse.CartItemResponse.builder()
                        .itemId(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .sellerId(item.getSellerId())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        // lineTotal entity metodundan alınır
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return CartResponse.builder()
                .cartId(cart.getId())
                .customerId(cart.getCustomerId())
                .items(itemResponses)
                // totalAmount entity metodundan alınır
                .totalAmount(cart.getTotalAmount())
                .totalItemCount(itemResponses.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}

