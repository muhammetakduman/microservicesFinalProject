package com.microservices.api_gateway.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway JWT Auth Filter — Her istekte çalışır.
 *
 * Akış:
 *  1. Authorization header'dan Bearer token çıkart
 *  2. JwtService ile doğrula (imza + süre)
 *  3. Claim'leri SecurityContext'e yükle
 *  4. Downstream servislere X-User-* header'larını ilet
 *     → downstream servisler JWT görmez, sadece bu header'lara güvenir
 *
 * Public path'ler (SecurityConfig'te tanımlı) bu filtreye takılmaz.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Bu path'ler için JWT filtresi ÇALIŞMAZ — Security config'deki permitAll ile uyumlu.
     * Hem gereksiz token kontrolünü engeller hem de 403 riskini ortadan kaldırır.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // OPTIONS (CORS preflight) her zaman bypass
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // Public path'ler — token kontrolü yapılmaz
        return path.startsWith("/api/auth/register")
            || path.equals("/api/auth/login")
            || path.equals("/api/auth/refresh-token")
            || path.startsWith("/actuator")
            || ("GET".equalsIgnoreCase(method) && path.startsWith("/api/products"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // -------------------------
        // 1. Authorization header oku
        // -------------------------
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Header yok → filtreden geç (Security config halleder: public mi, protected mı?)
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7); // "Bearer " prefix'ini sil

        // -------------------------
        // 2. Token doğrula
        // -------------------------
        if (!jwtService.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }

        // -------------------------
        // 3. Claim'leri çıkart
        // -------------------------
        Claims claims = jwtService.validateAndExtractClaims(token);
        String userId = claims.getSubject();                        // sub = userId
        String email  = claims.get("email", String.class);
        String role   = claims.get("role", String.class);          // "CUSTOMER", "SELLER", "ADMIN"

        // -------------------------
        // 4. SecurityContext'e yükle
        //    → Spring Security endpointlerinde @PreAuthorize çalışabilir
        // -------------------------
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // -------------------------
        // 5. Downstream header'larını ekle
        //    → Mikroservisler "Authorization: Bearer ..." görmez,
        //       sadece X-User-* header'larını okur (güvenli iç hat)
        // -------------------------
        // MutableHttpServletRequest ile yeni header ekle
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        mutableRequest.putHeader("X-User-Id",    userId);
        mutableRequest.putHeader("X-User-Email", email != null ? email : "");
        mutableRequest.putHeader("X-User-Role",  role  != null ? role  : "");

        filterChain.doFilter(mutableRequest, response);
    }
}

