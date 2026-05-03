package com.microservices.auth_service.repository;

import com.microservices.auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * RefreshToken CRUD repository.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Token değerine göre refresh token bulur.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Kullanıcının tüm refresh token'larını sil (logout / şifre değişimi).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}

