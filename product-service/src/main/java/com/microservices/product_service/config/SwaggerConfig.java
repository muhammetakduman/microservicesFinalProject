package com.microservices.product_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI productServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Service API")
                        .description("Marketplace - Urun ve Kategori Yonetimi")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Marketplace Team")
                                .email("info@marketplace.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8084").description("Local"),
                        new Server().url("http://api-gateway:8080").description("Docker")
                ));
    }
}

