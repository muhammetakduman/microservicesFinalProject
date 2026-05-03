package com.microservices.user_service.exception;

/**
 * Aynı e-posta ile ikinci kez profil oluşturulmaya çalışıldığında fırlatılır.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("Bu e-posta ile kayıtlı bir profil zaten mevcut: " + email);
    }
}

