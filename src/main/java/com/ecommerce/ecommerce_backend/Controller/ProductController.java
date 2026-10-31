package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Services.ProductService;
import com.ecommerce.ecommerce_backend.dto.request.ProductRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")          // better plural
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.toLowerCase()) {
            case "new"        -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "trending"   -> Sort.by(Sort.Direction.DESC, "ratingCount");
            case "featured"   -> Sort.by(Sort.Direction.DESC, "rating");
            case "price_asc"  -> Sort.by(Sort.Direction.ASC,  "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
    // Get all products
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching all products - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<ProductResponse> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    // Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        log.info("Fetching product by id: {}", id);
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    // Search products
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false, name = "productSize") String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String fit,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean newOnly,           // ✅ NEW
            @PageableDefault(size = 10) Pageable pageable) {

        Sort sortBy = resolveSort(sort);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortBy
        );

        Page<ProductResponse> products = productService.searchProducts(
                name, minPrice, maxPrice, categoryId,
                brand, size, color, fit, subcategory,
                newOnly,                                                // ✅ NEW
                sortedPageable);

        return ResponseEntity.ok(ApiResponse.success(products, "Products searched successfully"));
    }
//    @GetMapping("/search")
//    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) BigDecimal minPrice,
//            @RequestParam(required = false) BigDecimal maxPrice,
//            @RequestParam(required = false) Long categoryId,
//            @RequestParam(required = false) String brand,
//            @RequestParam(required = false) String subcategory,
//            @RequestParam(required = false, name = "productSize") String size,
//            @RequestParam(required = false) String color,
//            @RequestParam(required = false) String fit,
//            @RequestParam(required = false) String sort,
//            @PageableDefault(size = 10) Pageable pageable) {
//
//        Sort sortBy = resolveSort(sort);
//        Pageable sortedPageable = PageRequest.of(
//                pageable.getPageNumber(),
//                pageable.getPageSize(),
//                sortBy
//        );
//
//
//        log.info("Searching products - name: {}, minPrice: {}, maxPrice: {}, categoryId: {}, brand: {}, productSize: {}, color: {}, fit: {}, subcategory: {}",
//                name, minPrice, maxPrice, categoryId, brand, size, color, fit, subcategory);
//
//        Page<ProductResponse> products = productService.searchProducts(
//                name, minPrice, maxPrice, categoryId,
//                brand, size, color, fit, subcategory,
//                pageable, sortedPageable);
//
//        return ResponseEntity.ok(ApiResponse.success(products, "Products searched successfully"));
//    }

    // Get products by category
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("Fetching products by category id: {}", categoryId);
        Page<ProductResponse> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved by category successfully"));
    }

    // Get only active products
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getActiveProducts(
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 10) Pageable pageable) {

        Sort sortBy = resolveSort(sort);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortBy
        );

        log.info("Fetching active products");
        Page<ProductResponse> products = productService.getActiveProducts(sortedPageable);
        return ResponseEntity.ok(ApiResponse.success(products, "Active products retrieved successfully"));
    }

    // Create product (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        log.info("Creating new product: {}", request.getName());
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    // Update product (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        log.info("Updating product with id: {}", id);
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    // Soft delete product (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("Deleting (deactivating) product with id: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }

    // Activate product (Admin only)
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> activateProduct(@PathVariable Long id) {
        log.info("Activating product with id: {}", id);
        ProductResponse product = productService.activateProduct(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product activated successfully"));
    }

    // Deactivate product (Admin only)
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivateProduct(@PathVariable Long id) {
        log.info("Deactivating product with id: {}", id);
        ProductResponse product = productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product deactivated successfully"));
    }

    // Upload product image (Admin only)
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        log.info("Uploading image for product id: {}", id);
        ProductResponse product = productService.uploadProductImage(id, file);
        return ResponseEntity.ok(ApiResponse.success(product, "Product image uploaded successfully"));
    }

    // Delete product image (Admin only)
    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> deleteProductImage(@PathVariable Long id) {
        log.info("Deleting image for product id: {}", id);
        ProductResponse product = productService.deleteProductImage(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product image deleted successfully"));
    }
}