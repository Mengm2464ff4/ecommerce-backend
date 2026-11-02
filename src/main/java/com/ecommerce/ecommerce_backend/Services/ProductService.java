package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.ProductRequest;
import com.ecommerce.ecommerce_backend.dto.response.ProductResponse;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.model.Category;
import com.ecommerce.ecommerce_backend.model.Product;
import com.ecommerce.ecommerce_backend.Repositories.CategoryRepository;
import com.ecommerce.ecommerce_backend.Repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        products.getContent().forEach(p -> {
            if (p.getCategory() != null)
                p.getCategory().getName();
        });
        return products.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long categoryId,
            String brand,
            String size,
            String color,
            String fit,
            String subcategory,
            Boolean newOnly,
            Pageable pageable) {

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            preds.add(cb.isTrue(root.get("isActive")));

            // ✅ NEW — only products added in the last 7 days
//            if (Boolean.TRUE.equals(newOnly)) {
//                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
//                preds.add(cb.greaterThanOrEqualTo(
//                        root.get("createdAt"), sevenDaysAgo));
//            }
            // ✅ NEW — only products added in the last 365 days
            if (Boolean.TRUE.equals(newOnly)) {
                LocalDateTime oneYearAgo = LocalDateTime.now().minusDays(365);
                preds.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), oneYearAgo));
            }

            if (name != null && !name.isBlank()) {
                List<Predicate> namePreds = new ArrayList<>();
                for (String n : name.split(",")) {
                    namePreds.add(cb.like(
                            cb.lower(root.get("name")),
                            "%" + n.trim().toLowerCase() + "%"
                    ));
                }
                preds.add(cb.or(namePreds.toArray(new Predicate[0])));
            }

            if (minPrice != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (categoryId != null) {
                preds.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (brand != null && !brand.isBlank()) {
                List<Predicate> ps = new ArrayList<>();
                for (String b : brand.split(",")) {
                    ps.add(cb.equal(cb.lower(root.get("brand")), b.trim().toLowerCase()));
                }
                preds.add(cb.or(ps.toArray(new Predicate[0])));
            }

            if (size != null && !size.isBlank()) {
                List<Predicate> ps = new ArrayList<>();
                for (String s : size.split(",")) {
                    ps.add(cb.equal(cb.upper(root.get("size")), s.trim().toUpperCase()));
                }
                preds.add(cb.or(ps.toArray(new Predicate[0])));
            }

            if (color != null && !color.isBlank()) {
                List<Predicate> ps = new ArrayList<>();
                for (String c : color.split(",")) {
                    ps.add(cb.equal(cb.lower(root.get("color")), c.trim().toLowerCase()));
                }
                preds.add(cb.or(ps.toArray(new Predicate[0])));
            }

            if (fit != null && !fit.isBlank()) {
                List<Predicate> ps = new ArrayList<>();
                for (String f : fit.split(",")) {
                    ps.add(cb.equal(cb.lower(root.get("fit")), f.trim().toLowerCase()));
                }
                preds.add(cb.or(ps.toArray(new Predicate[0])));
            }
            if (subcategory != null && !subcategory.isBlank()) {
                List<Predicate> ps = new ArrayList<>();
                for (String sc : subcategory.split(",")) {
                    ps.add(cb.equal(cb.lower(root.get("subcategory")), sc.trim().toLowerCase()));
                }
                preds.add(cb.or(ps.toArray(new Predicate[0])));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };

        Page<Product> products = productRepository.findAll(spec, pageable);

        products.getContent().forEach(p -> {
            if (p.getCategory() != null) p.getCategory().getName();
        });

        return products.map(this::convertToResponse);
    }
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
        products.getContent().forEach(p -> {
            if (p.getCategory() != null) {
                p.getCategory().getName();
            }
        });
        return products.map(this::convertToResponse);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(category)
                .isActive(true)                    // ← FIXED (was .isActive(true))
                .brand(request.getBrand())   // ← ADD
                .size(request.getSize())     // ← ADD
                .color(request.getColor())   // ← ADD
                .fit(request.getFit())       // ← ADD
                .rating(request.getRating() != null ? request.getRating() : BigDecimal.ZERO)   // ← ADD
                .ratingCount(request.getRatingCount() != null ? request.getRatingCount() : 0)  // ← ADD
                .badge(request.getBadge()) // // 👈 ADD
                .discountPercent(request.getDiscountPercent())    // 👈 ADD
                .imageUrl(request.getImageUrl()) // 👈 ADD IT HERE
                .subcategory(request.getSubcategory()) // ADD NEW
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created product: {}", savedProduct.getName());
        return convertToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setBrand(request.getBrand());   // ← ADD
        product.setSize(request.getSize());     // ← ADD
        product.setColor(request.getColor());   // ← ADD
        product.setFit(request.getFit());
        product.setSubcategory(request.getSubcategory()); // ← ADD
        product.setSubcategory(request.getSubcategory());
        product.setRating(request.getRating() != null ? request.getRating() : BigDecimal.ZERO);
        product.setBadge(request.getBadge());
        product.setDiscountPercent(request.getDiscountPercent());
        product.setRatingCount(request.getRatingCount() != null ? request.getRatingCount() : 0);

        Product updatedProduct = productRepository.save(product);
        log.info("Updated product: {}", updatedProduct.getName());
        if (request.getImageUrl() != null && request.getImageUrl().startsWith("http")) {
            product.setImageUrl(request.getImageUrl());
            updatedProduct = productRepository.save(product);
        }
        return convertToResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);      // ← hard delete
        log.info("Deleted product: {}", product.getName());
    }

    @Transactional
    public ProductResponse uploadProductImage(Long id, MultipartFile file) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            // ✅ Absolute path from the project root
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "products");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            // ✅ Log the actual saved location so we can verify
            log.info("Saved image to: {}", filePath.toAbsolutePath());

            String imageUrl = "/uploads/products/" + fileName;
            product.setImageUrl(imageUrl);

            Product updatedProduct = productRepository.save(product);
            log.info("Uploaded image for product: {}", product.getName());
            return convertToResponse(updatedProduct);

        } catch (IOException e) {
            log.error("Failed to upload image", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    // ==================== MISSING METHODS ====================

    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByIsActiveTrue(pageable);
        products.getContent().forEach(p -> {
            if (p.getCategory() != null) {
                p.getCategory().getName();
            }
        });
        return products.map(this::convertToResponse);
    }

    @Transactional
    public ProductResponse activateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setActive(true);
        Product updated = productRepository.save(product);
        log.info("Activated product: {}", updated.getName());
        return convertToResponse(updated);
    }

    @Transactional
    public ProductResponse deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setActive(false);
        Product updated = productRepository.save(product);
        log.info("Deactivated product: {}", updated.getName());
        return convertToResponse(updated);
    }

    @Transactional
    public ProductResponse deleteProductImage(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (product.getImageUrl() != null) {
            try {
                // imageUrl looks like: /uploads/products/xxx.jpg
                String relative = product.getImageUrl().replaceFirst("^/uploads/", "");
                Path imagePath = Paths.get(System.getProperty("user.dir"), "uploads", relative);
                Files.deleteIfExists(imagePath);
                log.info("Deleted file: {}", imagePath.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to delete image file: {}", product.getImageUrl());
            }
        }

        product.setImageUrl(null);
        Product updated = productRepository.save(product);
        log.info("Deleted image for product: {}", product.getName());
        return convertToResponse(updated);
    }

    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .brand(product.getBrand())
                .size(product.getSize())
                .color(product.getColor())
                .fit(product.getFit())
                .subcategory(product.getSubcategory()) // 👈 KEEP THIS
                .badge(product.getBadge())
                .discountPercent(product.getDiscountPercent())
                .rating(product.getRating())
                .ratingCount(product.getRatingCount())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .isActive(product.isActive())
                .createdAt(product.getCreatedAt())
                .build(); // 👈 END HERE
    }


}