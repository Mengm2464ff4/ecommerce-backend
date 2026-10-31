package com.ecommerce.ecommerce_backend.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String brand;
    private String size;
    private String color;
    private String fit;
    private String subcategory;
    private String badge;
    private Integer discountPercent;
    private BigDecimal rating;
    private Integer ratingCount;
}
