package com.poultry.product.repository;

import com.poultry.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.deletedAt IS NULL")
    List<Product> findBySellerIdAndNotDeleted(UUID sellerId);

    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.status = :status AND p.deletedAt IS NULL")
    List<Product> findBySellerIdAndStatusAndNotDeleted(UUID sellerId, Product.ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.deletedAt IS NULL")
    Page<Product> findBySellerIdPageable(UUID sellerId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndNotDeleted(UUID id);

    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.sku = :sku AND p.deletedAt IS NULL")
    Optional<Product> findBySellerIdAndSku(UUID sellerId, String sku);

    boolean existsBySellerIdAndSku(UUID sellerId, String sku);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    Page<Product> findActiveByCategory(UUID categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL " +
            "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.sellerId = :sellerId AND p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    Page<Product> findActiveBySellerPaged(UUID sellerId, Pageable pageable);
}
