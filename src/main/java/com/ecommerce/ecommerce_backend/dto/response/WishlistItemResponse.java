package com.ecommerce.ecommerce_backend.dto.response;

import com.ecommerce.ecommerce_backend.model.Wishlist;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class  WishlistItemResponse {

    private Long id;
    private LocalDateTime createdAt;

    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private Integer productDiscountPercent;
    private String productBrand;
    private String productSize;
    private String productColor;
    private String productFit;
    private String productSubcategory;
    private BigDecimal productRating;
    private Integer productRatingCount;
    private String productBadge;
    private Integer productStock;

    private Long categoryId;
    private String categoryName;

    public static WishlistItemResponse from(Wishlist w) {
        var p = w.getProduct();
        var c = p != null ? p.getCategory() : null;
        return WishlistItemResponse.builder()
                .id(w.getId())
                .createdAt(w.getCreatedAt())
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .productImageUrl(p != null ? p.getImageUrl() : null)
                .productPrice(p != null ? p.getPrice() : null)
                .productDiscountPercent(p != null ? p.getDiscountPercent() : null)
                .productBrand(p != null ? p.getBrand() : null)
                .productSize(p != null ? p.getSize() : null)
                .productColor(p != null ? p.getColor() : null)
                .productFit(p != null ? p.getFit() : null)
                .productSubcategory(p != null ? p.getSubcategory() : null)
                .productRating(p != null ? p.getRating() : null)
                .productRatingCount(p != null ? p.getRatingCount() : null)
                .productBadge(p != null ? p.getBadge() : null)
                .productStock(p != null ? p.getStock() : null)
                .categoryId(c != null ? c.getId() : null)
                .categoryName(c != null ? c.getName() : null)
                .build();
    }
}