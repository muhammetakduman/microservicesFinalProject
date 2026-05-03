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

    @Operation(summary = "ID ile kategori getir")
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "Kategori ID") @PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getCategoryById(categoryId));
    }

    @Operation(summary = "Yeni kategori olustur (Admin)",
               description = "Yeni kategori ekler. Kategori adı sistemde benzersiz olmalıdır.")
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createCategory(request));
    }

    @Operation(summary = "Kategori guncelle (Admin)",
               description = "Kategori adını ve açıklamasını günceller.")
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @Parameter(description = "Kategori ID") @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(productService.updateCategory(categoryId, request));
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

    /**
     * POST /api/products/seller
     * Yeni ürün ekler. Ürün PENDING durumunda oluşur, admin onaylayana kadar listede çıkmaz.
     *
     * Kategori seçenekleri (birini gönder):
     *   - "categoryId": 5            → Mevcut kategorinin ID'si
     *   - "categoryName": "Elektronik" → İsimle bul (yoksa otomatik oluştur)
     *   - İkisi de yoksa             → "Genel" kategorisine atanır
     *
     * sellerId: JWT içindeki X-User-Id header'ından otomatik alınır.
     */
    @Operation(summary = "Yeni urun ekle (Seller)",
               description = "categoryId veya categoryName gönderilebilir; ikisi de yoksa 'Genel' kategorisine atanır")
    @PostMapping("/seller")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            @Parameter(description = "Satici ID (JWT'den)") @RequestHeader("X-Seller-Id") Long sellerId) {
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
