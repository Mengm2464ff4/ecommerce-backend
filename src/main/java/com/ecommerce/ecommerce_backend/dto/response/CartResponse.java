package com.ecommerce.ecommerce_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {


    private Long id;
    private Long userId;
    private String username;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private Integer totalItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
        private String size;   // 👈 ADD THIS
        // @Builder handles the rest
    }
}

