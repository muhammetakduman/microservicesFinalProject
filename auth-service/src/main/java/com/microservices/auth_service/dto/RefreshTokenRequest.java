package com.microservices.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Yeni access token talebi.
 * POST /api/auth/refresh-token
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    private String refreshToken;
}

