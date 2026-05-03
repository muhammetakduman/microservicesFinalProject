package com.microservices.stok_service.service;

import com.microservices.stok_service.entity.ProductStock;
import com.microservices.stok_service.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Uygulama ayağa kalkınca örnek stok verisi yükler.
 * Sadece veritabanı boşsa çalışır (idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockInitDataRunner implements ApplicationRunner {

    private final ProductStockRepository productStockRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (productStockRepository.count() > 0) {
            log.info("Stok tablosunda veri mevcut, init-data atlandı.");
            return;
        }

        List<ProductStock> initialStocks = List.of(
                ProductStock.builder()
                        .productId(1L)
                        .productName("Laptop Pro 15")
                        .sellerId(1L)
                        .availableQuantity(50)
                        .reservedQuantity(0)
                        .build(),
                ProductStock.builder()
                        .productId(2L)
                        .productName("Kablosuz Kulaklık")
                        .sellerId(1L)
                        .availableQuantity(200)
                        .reservedQuantity(0)
                        .build(),
                ProductStock.builder()
                        .productId(3L)
                        .productName("Mekanik Klavye")
                        .sellerId(2L)
                        .availableQuantity(100)
                        .reservedQuantity(0)
                        .build(),
                ProductStock.builder()
                        .productId(4L)
                        .productName("Gaming Mouse")
                        .sellerId(2L)
                        .availableQuantity(150)
                        .reservedQuantity(0)
                        .build(),
                ProductStock.builder()
                        .productId(5L)
                        .productName("USB-C Hub")
                        .sellerId(3L)
                        .availableQuantity(75)
                        .reservedQuantity(0)
                        .build()
        );

        productStockRepository.saveAll(initialStocks);
        log.info("Init-data: {} adet stok kaydı oluşturuldu.", initialStocks.size());
    }
}

