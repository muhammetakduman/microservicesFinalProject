package com.microservices.shopping_cart_service.repository;

import com.microservices.shopping_cart_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * CartItem CRUD repository.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Belirli sepetin tüm kalemlerini getirir.
     *
     * @param cartId Sepet ID'si
     * @return Sepetin kalemleri
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * Belirli bir sepette belirli bir ürün var mı?
     *
     * @param cartId    Sepet ID'si
     * @param productId Ürün ID'si
     * @return Kalem (varsa)
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Belirli bir sepetteki belirli ürünü sil.
     *
     * @param cartId    Sepet ID'si
     * @param productId Ürün ID'si
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.productId = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    /**
     * Bir sepetin tüm kalemlerini sil (sepet temizleme).
     *
     * @param cartId Sepet ID'si
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}

