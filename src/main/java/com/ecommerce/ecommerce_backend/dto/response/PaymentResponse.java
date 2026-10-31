package com.ecommerce.ecommerce_backend.dto.response;

import com.ecommerce.ecommerce_backend.model.PaymentStatus;
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
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String stripePaymentId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}
