package com.ecommerce.ecommerce_backend.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock is required.")
    @Min(value = 0, message = "Stock cannot be negative.")
    private Integer stock;

    @NotNull(message = "Category ID is required.")
    private Long categoryId;

    private String imageUrl;
    // ============ NEW FIELDS ============
    private String brand;
    private String size;
    private String color;
    private String fit;
    private String subcategory;
    private BigDecimal rating;
    private String badge;
    private Integer discountPercent;
    private Integer ratingCount;
}
