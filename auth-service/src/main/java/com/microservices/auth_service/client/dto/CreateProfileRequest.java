package com.microservices.auth_service.client.dto;

import lombok.*;

/**
 * user-service'e Feign ile göndereceğimiz profil oluşturma isteği.
 * user-service'deki CreateUserRequest ile aynı yapı.
 *
 * Neden ayrı class?
 *   Servisler arası shared library yok — her servis kendi DTO'sunu tanımlar.
 *   JSON payload yapısı aynı olduğu sürece uçtan uca çalışır.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProfileRequest {

    /** auth-service'den gelen userId — user-service bunu primary key olarak saklar */
    private Long userId;

    private String email;
    private String firstName;
    private String lastName;
    private String phone;
}

