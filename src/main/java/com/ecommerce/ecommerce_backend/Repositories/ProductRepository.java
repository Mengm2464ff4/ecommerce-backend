package com.ecommerce.ecommerce_backend.Repositories;

import com.ecommerce.ecommerce_backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;   // ← NEW import
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {   // ← ADD THIS

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    long countByCategoryId(Long categoryId);

    // ⬇️ ADD THIS — needed for cascade delete
    @Modifying
    @Query("DELETE FROM Product p WHERE p.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> searchByNameAndFilters(
            @Param("name") String name,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> searchByFilters(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}