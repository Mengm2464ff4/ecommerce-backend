package com.ecommerce.ecommerce_backend.Repositories;

import com.ecommerce.ecommerce_backend.model.Order;
import com.ecommerce.ecommerce_backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository <Payment, Long>{

    // Find payment by order ID
    Optional<Payment> findByOrder (Order order);

    // Find payment by order ID
    Optional<Payment> findByOrderId (Long orderId);

    // Find payment by Stripe payment ID
    Optional<Payment> findByStripePaymentId (String StripePaymentId);

    //
    Optional<Payment> findByTransactionId(String transactionId);

    //Check if payment exists for an order
    boolean existsByOrderId (long orderId);
}
