package com.microservices.shopping_cart_service.repository;

import com.microservices.shopping_cart_service.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * ShoppingCart CRUD repository.
 */
public interface CartRepository extends JpaRepository<ShoppingCart, Long> {

    /**
     * Müşterinin sepetini getirir.
     * Her müşterinin tek bir sepeti olduğu için Optional döner.
     *
     * @param customerId Müşteri ID'si
     * @return Müşterinin sepeti (varsa)
     */
    Optional<ShoppingCart> findByCustomerId(Long customerId);

    /**
     * Müşterinin sepeti var mı?
     *
     * @param customerId Müşteri ID'si
     * @return true: sepet mevcut
     */
    boolean existsByCustomerId(Long customerId);
}

