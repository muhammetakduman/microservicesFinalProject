package com.microservices.product_service.repository;

import com.microservices.product_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Büyük/küçük harf duyarsız isimle kategori bul */
    java.util.Optional<Category> findByNameIgnoreCase(String name);
}

