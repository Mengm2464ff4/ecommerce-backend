package com.ecommerce.ecommerce_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String billingAddress;

    @NotBlank(message = "Payment method is required.")
    private String paymentMethod;

    private List<OrderItemRequest> items;

}
