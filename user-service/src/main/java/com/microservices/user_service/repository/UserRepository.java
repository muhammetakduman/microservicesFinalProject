package com.microservices.user_service.repository;

import com.microservices.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User profil CRUD repository.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * E-posta ile kullanıcı profili bulur.
     * auth-service profil oluştururken e-posta çakışmasını kontrol eder.
     *
     * @param email Kullanıcı e-postası
     * @return Profil (varsa)
     */
    Optional<User> findByEmail(String email);

    /**
     * Bu e-posta ile kayıtlı profil var mı?
     * Tekrar kayıt kontrolü için kullanılır.
     *
     * @param email Kullanıcı e-postası
     * @return true: kayıtlı
     */
    boolean existsByEmail(String email);
}

