package com.microservices.auth_service.security;

import com.microservices.auth_service.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security'nin kullanıcı yükleme arayüzü implementasyonu.
 *
 * Spring Security, login işleminde usernı (email) kullanarak bu servisi çağırır.
 * Dönen UserDetails nesnesi AuthUser entity'sidir (UserDetails implement ediyor).
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    /**
     * E-posta ile kullanıcıyı yükler.
     * Spring Security authentication sürecinde çağrılır.
     *
     * @param email Kullanıcı e-postası (username olarak kullanılıyor)
     * @return UserDetails (AuthUser entity'si)
     * @throws UsernameNotFoundException Kullanıcı bulunamazsa
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return authUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Kullanıcı bulunamadı – email: " + email));
    }
}

