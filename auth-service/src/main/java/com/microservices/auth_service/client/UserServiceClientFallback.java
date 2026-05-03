package com.microservices.auth_service.client;

import com.microservices.auth_service.client.dto.CreateProfileRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * UserServiceClient için Circuit Breaker fallback.
 *
 * user-service cevap vermezse kayıt tamamen başarısız olmak yerine
 * bu fallback devreye girer ve sadece log bırakır.
 *
 * Gerçek uygulamada: kullanıcıya "Profil daha sonra oluşturulacak"
 * mesajı gösterilebilir veya event queue'ya alınabilir.
 */
@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public void createProfile(CreateProfileRequest request) {
        // user-service ulaşılamaz — sadece log bırak, kayıt iptal edilMEZ
        log.error("user-service ulaşılamadı! Profil oluşturulamadı – userId: {}, email: {}",
                request.getUserId(), request.getEmail());
    }
}

