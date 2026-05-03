package com.microservices.auth_service.dto;

import com.microservices.auth_service.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Admin kullanıcı rolü güncelleme isteği.
 * PUT /api/auth/admin/users/{userId}/role
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateRequest {

    @NotNull(message = "Rol boş olamaz")
    private Role role;
}

