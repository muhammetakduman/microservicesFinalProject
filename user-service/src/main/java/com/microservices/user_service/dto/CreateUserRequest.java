package com.microservices.user_service.dto;

import com.microservices.user_service.entity.UserAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Yeni kullanıcı profili oluşturma isteği.
 *
 * Kim çağırır?
 *   auth-service → kullanıcı kayıt olunca bu endpoint'i çağırır.
 *   POST /api/users/profile
 *
 * id: auth-service'deki kullanıcının ID'si — user-service bu ID'yi PK olarak kullanır.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    /**
     * auth-service'den gelen kullanıcı ID'si.
     * user-service bu ID'yi primary key olarak kaydeder.
     * Böylece iki servis arasında ID tutarlılığı sağlanır.
     */
    @NotNull(message = "userId boş olamaz")
    private Long userId;

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi girin")
    private String email;

    @NotBlank(message = "Ad boş olamaz")
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    private String lastName;

    /** Opsiyonel — kayıt sırasında girilmeyebilir */
    private String phone;
}

