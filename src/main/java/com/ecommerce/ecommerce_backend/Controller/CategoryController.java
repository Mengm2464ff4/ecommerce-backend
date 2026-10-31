package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Services.CategoryService;
import com.ecommerce.ecommerce_backend.dto.request.CategoryRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")          // ✅ FIXED — was "/api/categor"
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getAllCategories(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        log.info("Fetching all categories with pagination");
        Page<CategoryResponse> categories = categoryService.getAllCategories(pageable);
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully!"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesList() {
        log.info("Fetching all categories as list");
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        log.info("Fetching category by id: {}", id);
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully!"));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryByName(@PathVariable String name) {
        log.info("Fetching category by name: {}", name);
        CategoryResponse category = categoryService.getCategoryByName(name);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")           // ✅ FIXED — was "hashRole"
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest categoryRequest) {
        log.info("Creating new category: {}", categoryRequest.getName());
        CategoryResponse category = categoryService.createCategory(categoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")           // ✅ FIXED — was "hashRole"
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest categoryRequest
    ) {
        log.info("Updating category with id: {}", id);
        CategoryResponse category = categoryService.updateCategory(id, categoryRequest);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        log.info("Deleting category with id: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkCategoryExists(@RequestParam String name) {
        log.info("Checking if category exists with name: {}", name);
        boolean exists = categoryService.existsByName(name);
        return ResponseEntity.ok(ApiResponse.success(exists, "Category existence checked."));
    }
}