package com.microservices.auth_service.client;

import com.microservices.auth_service.client.dto.CreateProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * user-service OpenFeign istemcisi.
 *
 * Kullanım senaryosu:
 *   Yeni kullanıcı kayıt olduğunda auth-service, user-service'e
 *   profil oluşturma isteği gönderir.
 *
 * name = "user-service" → Eureka'da bu isimle kayıtlı servisi bulur.
 * Eureka çalışmıyorsa fallback url kullanılır.
 */
@FeignClient(
        name = "user-service",
        url = "${user-service.url:}",   // Boş → Eureka kullanır; dolu → direkt çağrı
        fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    /**
     * Yeni kullanıcı profili oluştur.
     * auth-service kayıt akışında çağırır.
     *
     * @param request userId, email, firstName, lastName, phone
     */
    @PostMapping("/api/users/profile")
    void createProfile(@RequestBody CreateProfileRequest request);
}

