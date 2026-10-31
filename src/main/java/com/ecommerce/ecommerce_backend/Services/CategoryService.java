// src/main/java/com/ecommerce/service/CategoryService.java
package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.CategoryRequest;
import com.ecommerce.ecommerce_backend.dto.response.CategoryResponse;
import com.ecommerce.ecommerce_backend.exception.CustomException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.model.Category;
import com.ecommerce.ecommerce_backend.Repositories.CategoryRepository;
import com.ecommerce.ecommerce_backend.Repositories.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return convertToResponse(category);
    }

    public CategoryResponse getCategoryByName(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "name", name));
        return convertToResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // Check if category name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw CustomException.conflict("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created: {}", savedCategory.getName());

        return convertToResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if another category with the same name exists
        categoryRepository.findByName(request.getName())
                .ifPresent(existingCategory -> {
                    if (!existingCategory.getId().equals(category.getId())) {
                        throw CustomException.conflict("Category with name '" + request.getName() + "' already exists");
                    }
                });

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated: {}", updatedCategory.getName());

        return convertToResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        categoryRepository.delete(category);
        log.info("Category deleted: {}", category.getName());
    }
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    private CategoryResponse convertToResponse(Category category) {
        long productCount = productRepository.countByCategoryId(category.getId());

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount((int) productCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}