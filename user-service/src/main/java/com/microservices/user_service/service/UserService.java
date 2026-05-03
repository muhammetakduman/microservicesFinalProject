package com.microservices.user_service.service;

import com.microservices.user_service.dto.CreateUserRequest;
import com.microservices.user_service.dto.UpdateUserRequest;
import com.microservices.user_service.dto.UserResponse;
import com.microservices.user_service.entity.User;
import com.microservices.user_service.exception.UserAlreadyExistsException;
import com.microservices.user_service.exception.UserNotFoundException;
import com.microservices.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Kullanıcı profil servisinin tüm iş mantığını barındırır.
 *
 * Sorumluluklar (SRP):
 *  - Profil oluşturma (auth-service kayıt akışı sırasında çağırır)
 *  - Profil okuma: kendi profili, ID ile sorgulama
 *  - Profil güncelleme
 *  - Admin: tüm kullanıcılar, kullanıcı aktif/pasif etme
 *
 * Bu servis kimlik doğrulama (şifre, JWT) ile ilgilenmez.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────
    // Profil Oluştur (auth-service çağırır)
    // ─────────────────────────────────────────────────────────────

    /**
     * Yeni kullanıcı profili oluşturur.
     *
     * Akış:
     *  auth-service → kayıt başarılı → POST /api/users/profile → bu metod
     *
     * @param request auth-service'den gelen profil bilgileri
     * @return Oluşturulan kullanıcı profili
     * @throws UserAlreadyExistsException Aynı e-posta ile profil zaten mevcutsa
     */
    @Transactional
    public UserResponse createProfile(CreateUserRequest request) {
        // E-posta çakışma kontrolü — idempotent davranmak için
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                // auth-service'den gelen ID primary key olarak kullanılır
                .id(request.getUserId())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .active(true)
                .build();

        userRepository.save(user);
        log.info("Kullanıcı profili oluşturuldu – userId: {}, email: {}",
                user.getId(), user.getEmail());

        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────
    // Profil Oku
    // ─────────────────────────────────────────────────────────────

    /**
     * Belirtilen ID'ye sahip kullanıcı profilini getirir.
     * GET /api/users/{userId} — diğer servisler Feign ile çağırır.
     *
     * @param userId Kullanıcı ID'si
     * @return Kullanıcı profili
     * @throws UserNotFoundException Kullanıcı bulunamazsa
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = findUser(userId);
        return toResponse(user);
    }

    /**
     * Belirtilen e-postaya sahip kullanıcı profilini getirir.
     * auth-service e-posta doğrulaması sırasında kullanabilir.
     *
     * @param email Kullanıcı e-postası
     * @return Kullanıcı profili
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────
    // Profil Güncelle (müşteri kendi profilini günceller)
    // ─────────────────────────────────────────────────────────────

    /**
     * Kullanıcı kendi profilini günceller.
     * PUT /api/users/me
     *
     * Null-safe güncelleme: sadece gelen alanlar değiştirilir.
     * email ve id değiştirilemez.
     *
     * @param userId  Güncellenecek kullanıcının ID'si (JWT'den gelir)
     * @param request Güncelleme isteği
     * @return Güncellenmiş profil
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);

        // Null-safe partial update — sadece gönderilen alanlar güncellenir
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getProfilePhotoUrl() != null) {
            user.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        userRepository.save(user);
        log.info("Kullanıcı profili güncellendi – userId: {}", userId);

        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────
    // Admin işlemleri
    // ─────────────────────────────────────────────────────────────

    /**
     * Tüm kullanıcı profillerini listeler (admin paneli).
     *
     * @return Tüm kullanıcılar
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Kullanıcı hesabını devre dışı bırakır.
     * Hesap silmek yerine soft-delete tercih edilir —
     * geçmiş siparişler, yorumlar vb. kayıtlar etkilenmez.
     *
     * @param userId Devre dışı bırakılacak kullanıcı ID'si
     * @return Güncellenmiş kullanıcı profili
     */
    @Transactional
    public UserResponse deactivateUser(Long userId) {
        User user = findUser(userId);
        // İş mantığı entity metodunda (Rich Domain Model)
        user.deactivate();
        userRepository.save(user);
        log.info("Kullanıcı devre dışı bırakıldı – userId: {}", userId);
        return toResponse(user);
    }

    /**
     * Devre dışı bırakılmış kullanıcı hesabını yeniden aktif eder.
     *
     * @param userId Aktif edilecek kullanıcı ID'si
     * @return Güncellenmiş kullanıcı profili
     */
    @Transactional
    public UserResponse activateUser(Long userId) {
        User user = findUser(userId);
        user.activate();
        userRepository.save(user);
        log.info("Kullanıcı aktif edildi – userId: {}", userId);
        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metodlar (private)
    // ─────────────────────────────────────────────────────────────

    /**
     * Kullanıcıyı ID ile bulur. Bulunamazsa exception fırlatır.
     * DRY: findUser tekrarlayan repository çağrısını tek noktada toplar.
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * User entity → response DTO dönüşümü.
     * Tüm mapping tek metotta — DRY ve kolayca değiştirilebilir.
     */
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                // fullName entity metodundan hesaplanır
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .address(user.getAddress())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

