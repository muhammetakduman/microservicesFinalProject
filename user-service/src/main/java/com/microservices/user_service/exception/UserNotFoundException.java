package com.microservices.user_service.exception;

/**
 * Kullanıcı profili bulunamadığında fırlatılır.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("Kullanıcı profili bulunamadı — userId: " + userId);
    }

    public UserNotFoundException(String email) {
        super("Kullanıcı profili bulunamadı — email: " + email);
    }
}

