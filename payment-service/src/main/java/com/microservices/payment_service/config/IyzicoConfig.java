package com.microservices.payment_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * iyzico API yapılandırması.
 *
 * Değerler config-server'dan okunur:
 *   Placeholder : payment-service.properties       → git'e gider (güvenli)
 *   Gerçek key  : payment-service-local.properties → .gitignore ile korunur
 *
 * Spring Cloud Config, profile=local olduğunda
 * payment-service-local.properties'i otomatik yükler ve override eder.
 */
@Configuration
@ConfigurationProperties(prefix = "iyzico")
@EnableConfigurationProperties
@Getter
@Setter
public class IyzicoConfig {

    /** Firma Ayarları → API Anahtarları bölümündeki API Key */
    private String apiKey;

    /** Firma Ayarları → API Anahtarları bölümündeki Güvenlik Anahtarı */
    private String secretKey;

    /** Sandbox: https://sandbox-api.iyzipay.com  |  Prod: https://api.iyzipay.com */
    private String baseUrl = "https://sandbox-api.iyzipay.com";
}

