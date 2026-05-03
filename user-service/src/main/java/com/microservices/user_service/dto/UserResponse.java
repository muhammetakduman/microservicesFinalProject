package com.microservices.user_service.dto;

import com.microservices.user_service.entity.UserAddress;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Kullanıcı profil cevap DTO'su.
 *
 * Tüm GET endpoint'lerinden döner.
 * Hassas alanlar (şifre vb.) zaten bu serviste yok — güvenli.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long userId;
    private String email;
    private String firstName;
    private String lastName;

    /** firstName + lastName birleşimi (entity metodundan hesaplanır) */
    private String fullName;

    private String phone;
    private String profilePhotoUrl;
    private UserAddress address;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

