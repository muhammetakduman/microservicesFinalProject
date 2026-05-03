package com.microservices.api_gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * API Gateway Security Konfigürasyonu
 *
 * Kural hiyerarşisi:
 *  PUBLIC   → kimlik doğrulaması gerektirmez
 *  AUTHENTICATED → geçerli JWT gerekir
 *  ADMIN    → ROLE_ADMIN gerekir (ilerleyen sürüm için hazır)
 *
 * JWT filtresi SecurityContext'i doldurur,
 * sonra Spring Security kuralları devreye girer.
 *
 * CORS: localhost:5173 (Vite/React) ve localhost:3000 (CRA) izin verilir.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * CORS Konfigürasyonu
     *
     * Frontend geliştirme ortamları:
     *   localhost:5173 → Vite (React, Vue)
     *   localhost:3000 → Create React App / Next.js
     *
     * İzin verilen method'lar: GET, POST, PUT, PATCH, DELETE, OPTIONS
     * OPTIONS → preflight istekleri için (tarayıcı önceden sorar)
     *
     * Authorization, X-Seller-Id gibi custom header'lara da izin verilir.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // İzin verilen frontend origin'ler
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",   // Vite
            "http://localhost:3000",   // CRA / Next.js
            "http://localhost:4200",
                "https://app.saforygroup.com",
                "https://saforygroup.com"
        ));

        // İzin verilen HTTP method'ları
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // İzin verilen header'lar
        config.setAllowedHeaders(List.of(
            "Authorization",    // JWT token
            "Content-Type",     // application/json
            "X-Seller-Id",      // Seller endpoint'leri için
            "X-User-Id",        // Gateway'in eklediği userId header'ı
            "X-User-Role"       // Gateway'in eklediği role header'ı
        ));

        // Frontend'in Authorization header'ını okuyabilmesi için
        config.setExposedHeaders(List.of("Authorization"));

        // Cookie / credentials gönderimini destekle
        config.setAllowCredentials(true);

        // Preflight cache süresi (saniye) — tarayıcı tekrar OPTIONS atmaz
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Tüm path'lere uygula
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // CSRF devre dışı (stateless REST API)
            .csrf(csrf -> csrf.disable())

            // CORS aktif et — yukarıdaki corsConfigurationSource bean'i kullanır
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Session yok — JWT ile stateless çalışıyoruz
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // =====================
                // PUBLIC ENDPOINTS
                // =====================

                // Auth: register, login, refresh token (JWT gerektirmez)
                // OPTIONS → tarayıcının preflight isteği de izinli olmalı
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh-token").permitAll()

                // Ürünleri listeleme / detay görme herkese açık
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                // Actuator health check (monitoring için)
                .requestMatchers("/actuator/**").permitAll()

                // =====================
                // AUTHENTICATED ENDPOINTS
                // =====================

                // Auth: logout (token gerekli)
                .requestMatchers("/api/auth/logout").authenticated()

                // Kullanıcı profili
                .requestMatchers("/api/users/**").authenticated()

                // Sepet işlemleri
                .requestMatchers("/api/cart/**").authenticated()

                // Sipariş işlemleri
                .requestMatchers("/api/orders/**").authenticated()

                // Ödeme işlemleri
                .requestMatchers("/api/payments/**").authenticated()

                // Bildirimler
                .requestMatchers("/api/notifications/**").authenticated()

                // Stok (admin/seller kullanır — ilerleyen sürümde role bazlı kısıtlanabilir)
                .requestMatchers("/api/stock/**").authenticated()

                // Ürün CRUD — sadece SELLER veya ADMIN yapabilir
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")

                // Belirsiz kalan tüm istekler → kimlik doğrulaması gerekir
                .anyRequest().authenticated()
            )

            // JWT filtresini UsernamePasswordAuthenticationFilter'dan ÖNCE koy
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

