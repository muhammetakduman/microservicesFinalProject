package com.microservices.auth_service.repository;

import com.microservices.auth_service.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * AuthUser kimlik bilgisi CRUD repository.
 */
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    /**
     * E-posta ile kullanıcı bulur — login ve kayıt çakışma kontrolünde kullanılır.
     */
    Optional<AuthUser> findByEmail(String email);

    /**
     * Bu e-posta zaten kayıtlı mı?
     */
    boolean existsByEmail(String email);
}

