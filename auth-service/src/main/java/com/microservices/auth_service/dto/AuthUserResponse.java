package com.microservices.auth_service.dto;

import com.microservices.auth_service.entity.Role;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Kullanıcı kimlik bilgisi özeti — Admin listeleme endpoint'i için.
 * Şifre hash'i ASLA döndürülmez.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserResponse {

    private Long id;
    private String email;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}

