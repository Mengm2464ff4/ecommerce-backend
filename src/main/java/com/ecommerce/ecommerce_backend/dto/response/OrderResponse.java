package com.ecommerce.ecommerce_backend.dto.response;

import com.ecommerce.ecommerce_backend.model.OrderStatus;
import com.ecommerce.ecommerce_backend.model.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime placedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    // Nested static class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private String productImageUrl;
    }
}