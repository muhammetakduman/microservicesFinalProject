package com.microservices.auth_service.entity;

/**
 * Kullanıcı rolleri.
 * docs/06-dev-rules.md — Security Rule:
 *   ADMIN   → Tam sistem erişimi
 *   SELLER  → Kendi ürünleri ve satışları
 *   CUSTOMER → Sepet ve siparişler
 */
public enum Role {
    ADMIN,
    SELLER,
    CUSTOMER
}

