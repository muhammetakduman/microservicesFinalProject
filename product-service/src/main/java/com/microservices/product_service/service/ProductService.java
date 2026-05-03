package com.microservices.product_service.service;

import com.microservices.product_service.config.RabbitMQConfig;
import com.microservices.product_service.dto.*;
import com.microservices.product_service.entity.Category;
import com.microservices.product_service.entity.Product;
import com.microservices.product_service.entity.Product.ProductStatus;
import com.microservices.product_service.event.StockDecreaseEvent;
import com.microservices.product_service.exception.CategoryNotFoundException;
import com.microservices.product_service.exception.ProductNotFoundException;
import com.microservices.product_service.mapper.CategoryMapper;
import com.microservices.product_service.mapper.ProductMapper;
import com.microservices.product_service.repository.CategoryRepository;
import com.microservices.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final String DEFAULT_CATEGORY_NAME = "Genel";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    // -------------------------
    // PUBLIC
    // -------------------------

    public Page<ProductResponse> getAllApprovedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepository.findByStatus(ProductStatus.APPROVED, pageable)
                .map(productMapper::toResponse);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toResponse(product);
    }

    public Page<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepository.findByNameContainingIgnoreCaseAndStatus(keyword, ProductStatus.APPROVED, pageable)
                .map(productMapper::toResponse);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    @Transactional
    public CategoryResponse createCategory(com.microservices.product_service.dto.CategoryRequest request) {
        com.microservices.product_service.entity.Category category =
                com.microservices.product_service.entity.Category.builder()
                        .name(request.getName())
                        .description(request.getDescription())
                        .build();
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
        categoryRepository.deleteById(categoryId);
    }

    // -------------------------
    // SELLER
    // -------------------------

    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long sellerId) {
        Category category = resolveCategory(request);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .sellerId(sellerId)
                .category(category)
                .status(ProductStatus.PENDING)
                .build();

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Bu urunu guncelleme yetkiniz yok.");
        }

        Category category = resolveCategory(request);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Bu urunu silme yetkiniz yok.");
        }

        productRepository.delete(product);
    }

    public Page<ProductResponse> getSellerProducts(Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepository.findBySellerId(sellerId, pageable)
                .map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponse updateStock(Long productId, StockUpdateRequest request, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Bu urunun stok guncelleme yetkiniz yok.");
        }

        product.setStock(request.getStock());
        return productMapper.toResponse(productRepository.save(product));
    }

    // -------------------------
    // ADMIN
    // -------------------------

    public Page<ProductResponse> getPendingProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepository.findByStatus(ProductStatus.PENDING, pageable)
                .map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponse approveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.setStatus(ProductStatus.APPROVED);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse rejectProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.setStatus(ProductStatus.REJECTED);
        return productMapper.toResponse(productRepository.save(product));
    }

    // -------------------------
    // Yardımcı: Kategori Çözümleme
    // -------------------------

    /**
     * Ürün isteğinden kategori belirler:
     *  1. categoryId varsa → doğrudan DB'den al (bulamazsa hata)
     *  2. categoryName varsa → isimle ara; yoksa otomatik oluştur
     *  3. İkisi de yoksa → "Genel" kategorisini bul/oluştur
     */
    @Transactional
    protected Category resolveCategory(ProductRequest request) {
        // 1. categoryId öncelikli
        if (request.getCategoryId() != null) {
            return categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
        }

        // 2. categoryName ile bul ya da oluştur
        String name = (request.getCategoryName() != null && !request.getCategoryName().isBlank())
                ? request.getCategoryName().trim()
                : DEFAULT_CATEGORY_NAME;

        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    log.info("Yeni kategori oluşturuluyor: '{}'", name);
                    return categoryRepository.save(
                            Category.builder()
                                    .name(name)
                                    .description("Otomatik oluşturuldu")
                                    .build()
                    );
                });
    }

    // -------------------------
    // RabbitMQ - Stok Dusurme
    // -------------------------

    @RabbitListener(queues = RabbitMQConfig.STOCK_DECREASE_QUEUE)
    @Transactional
    public void handleStockDecrease(StockDecreaseEvent event) {
        Product product = productRepository.findById(event.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(event.getProductId()));

        if (product.getStock() < event.getQuantity()) {
            throw new RuntimeException("Yetersiz stok. Urun ID: " + event.getProductId());
        }

        product.setStock(product.getStock() - event.getQuantity());
        productRepository.save(product);
    }
}
