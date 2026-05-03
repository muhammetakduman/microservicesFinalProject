package com.microservices.product_service.controller;

import com.microservices.product_service.dto.*;
import com.microservices.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product API", description = "Urun ve Kategori Yonetimi")
public class ProductController {

    private final ProductService productService;

    // -------------------------
    // PUBLIC
    // -------------------------

    @Operation(summary = "Tum onaylanmis urunleri listele", description = "Sayfalama desteklidir")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getAllApprovedProducts(page, size));
    }

    @Operation(summary = "ID ile urun getir")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Urun ID") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Urun ara", description = "Urun adina gore arama yapar")
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Arama kelimesi") @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.searchProducts(keyword, page, size));
    }

    @Operation(summary = "Tum kategorileri listele")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @Operation(summary = "Yeni kategori olustur (Admin)")
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createCategory(request));
    }

    @Operation(summary = "Kategori sil (Admin)")
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Kategori ID") @PathVariable Long categoryId) {
        productService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------
    // SELLER
    // -------------------------

    @Operation(summary = "Yeni urun ekle (Seller)", description = "Header: X-Seller-Id gereklidir")
    @PostMapping("/seller")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            @Parameter(description = "Satici ID") @RequestHeader("X-Seller-Id") Long sellerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request, sellerId));
    }

    @Operation(summary = "Urun guncelle (Seller)")
    @PutMapping("/seller/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Urun ID") @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request,
            @Parameter(description = "Satici ID") @RequestHeader("X-Seller-Id") Long sellerId) {
        return ResponseEntity.ok(productService.updateProduct(productId, request, sellerId));
    }

    @Operation(summary = "Urun sil (Seller)")
    @DeleteMapping("/seller/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Urun ID") @PathVariable Long productId,
            @Parameter(description = "Satici ID") @RequestHeader("X-Seller-Id") Long sellerId) {
        productService.deleteProduct(productId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Satici kendi urunlerini listele (Seller)")
    @GetMapping("/seller/my-products")
    public ResponseEntity<Page<ProductResponse>> getSellerProducts(
            @Parameter(description = "Satici ID") @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getSellerProducts(sellerId, page, size));
    }

    @Operation(summary = "Stok guncelle (Seller)")
    @PatchMapping("/seller/{productId}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @Parameter(description = "Urun ID") @PathVariable Long productId,
            @Valid @RequestBody StockUpdateRequest request,
            @Parameter(description = "Satici ID") @RequestHeader("X-Seller-Id") Long sellerId) {
        return ResponseEntity.ok(productService.updateStock(productId, request, sellerId));
    }

    // -------------------------
    // ADMIN
    // -------------------------

    @Operation(summary = "Onay bekleyen urunleri listele (Admin)")
    @GetMapping("/admin/pending")
    public ResponseEntity<Page<ProductResponse>> getPendingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getPendingProducts(page, size));
    }

    @Operation(summary = "Urunu onayla (Admin)")
    @PutMapping("/admin/{productId}/approve")
    public ResponseEntity<ProductResponse> approveProduct(
            @Parameter(description = "Urun ID") @PathVariable Long productId) {
        return ResponseEntity.ok(productService.approveProduct(productId));
    }

    @Operation(summary = "Urunu reddet (Admin)")
    @PutMapping("/admin/{productId}/reject")
    public ResponseEntity<ProductResponse> rejectProduct(
            @Parameter(description = "Urun ID") @PathVariable Long productId) {
        return ResponseEntity.ok(productService.rejectProduct(productId));
    }
}
