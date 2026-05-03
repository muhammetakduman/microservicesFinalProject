package com.microservices.auth_service.config;

import com.microservices.auth_service.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security yapılandırması.
 *
 * Tasarım kararları:
 * ─────────────────────────────────────────────────────────────────
 * 1. auth-service JWT ÜRETIR ama doğrulamaz — API Gateway doğrular.
 *    Bu nedenle JwtAuthFilter burada YOK.
 *
 * 2. Tüm /api/auth/** endpoint'leri herkese açıktır.
 *
 * 3. Stateless session — JWT tabanlı mimari için zorunlu.
 *    HttpSession kullanılmaz.
 *
 * 4. BCrypt — şifre hashleme için endüstri standardı.
 *    (Cost factor varsayılan 10 — 2^10 iterasyon)
 * ─────────────────────────────────────────────────────────────────
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    /**
     * HTTP güvenlik zinciri.
     * CSRF devre dışı — REST API, stateless, token tabanlı.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API için CSRF koruması gereksiz
                .csrf(AbstractHttpConfigurer::disable)

                // Session tutma — JWT stateless olduğu için SESSION YOK
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Endpoint yetkilendirmesi
                .authorizeHttpRequests(auth -> auth
                        // Tüm auth endpoint'leri herkese açık
                        .requestMatchers("/api/auth/**").permitAll()
                        // Diğer endpoint'ler (admin vb.) yetki gerektirir
                        .anyRequest().authenticated()
                )

                // DaoAuthenticationProvider'ı kullan
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    /**
     * Şifre encoder — BCrypt (OWASP önerisi).
     * Aynı şifre her seferinde farklı hash üretir (salt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security'nin kimlik doğrulama sağlayıcısı.
     * Email ile kullanıcı yükler, BCrypt ile şifre karşılaştırır.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        // BCrypt encoder ile şifre doğrulaması yapılır
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — service katmanında programatik login için kullanılır.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}

