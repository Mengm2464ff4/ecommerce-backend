// src/main/java/com/ecommerce/service/PaymentService.java
package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.PaymentRequest;
import com.ecommerce.ecommerce_backend.dto.response.PaymentResponse;
import com.ecommerce.ecommerce_backend.exception.CustomException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.model.Order;
import com.ecommerce.ecommerce_backend.model.Payment;
import com.ecommerce.ecommerce_backend.model.PaymentStatus;
import com.ecommerce.ecommerce_backend.Repositories.OrderRepository;
import com.ecommerce.ecommerce_backend.Repositories.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        // Check if payment already exists for this order
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw CustomException.conflict("Payment already exists for this order");
        }

        // Initialize Stripe
        Stripe.apiKey = stripeApiKey;

        try {
            // Create Stripe Payment Intent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                    .setCurrency("usd")
                    .setDescription("Order #" + order.getOrderNumber())
                    .addAllPaymentMethodType(java.util.Collections.singletonList("card"))   // ← fixed here
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Create payment record
            Payment payment = Payment.builder()
                    .order(order)
                    .stripePaymentId(paymentIntent.getId())
                    .amount(request.getAmount())
                    .status(PaymentStatus.PENDING)
                    .paymentMethod(request.getPaymentMethod())
                    .createdAt(LocalDateTime.now())
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment created with ID: {} for order: {}", savedPayment.getId(), order.getOrderNumber());

            return convertToResponse(savedPayment);

        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage(), e);
            throw CustomException.internalServerError("Payment processing failed: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse confirmPayment(String paymentIntentId) {
        Stripe.apiKey = stripeApiKey;

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            Payment payment = paymentRepository.findByStripePaymentId(paymentIntentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "stripePaymentId", paymentIntentId));

            // Update payment status based on Stripe response
            switch (paymentIntent.getStatus()) {
                case "succeeded":
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setTransactionId(paymentIntent.getId());
                    payment.setPaytmentDate(LocalDateTime.now());
                    payment.setPaymentMethod(paymentIntent.getPaymentMethodTypes().get(0));

                    // Update order payment status
                    Order order = payment.getOrder();
                    orderService.updatePaymentStatus(order.getId(), PaymentStatus.PAID);
                    break;

                case "requires_payment_method":
                case "requires_confirmation":
                case "requires_action":
                    payment.setStatus(PaymentStatus.PENDING);
                    break;

                case "canceled":
                    payment.setStatus(PaymentStatus.FAILED);
                    break;

                default:
                    payment.setStatus(PaymentStatus.FAILED);
            }

            Payment updatedPayment = paymentRepository.save(payment);
            log.info("Payment confirmed: {} with status: {}", paymentIntentId, payment.getStatus());

            return convertToResponse(updatedPayment);

        } catch (StripeException e) {
            log.error("Error confirming payment: {}", e.getMessage(), e);
            throw CustomException.internalServerError("Failed to confirm payment: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // First create the payment
        PaymentResponse paymentResponse = createPayment(request);

        try {
            // Confirm the payment
            return confirmPayment(paymentResponse.getStripePaymentId());
        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage(), e);
            Payment payment = paymentRepository.findByStripePaymentId(paymentResponse.getStripePaymentId())
                    .orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
            throw CustomException.internalServerError("Payment processing failed: " + e.getMessage());
        }
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return convertToResponse(payment);
    }

    public PaymentResponse getPaymentByStripeId(String stripePaymentId) {
        Payment payment = paymentRepository.findByStripePaymentId(stripePaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "stripePaymentId", stripePaymentId));
        return convertToResponse(payment);
    }

    // ==================== ADDED MISSING METHOD ====================
    public String getPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        return payment.getStatus().name(); // returns "PENDING", "PAID", "FAILED", "REFUNDED"...
    }
    // ===========

    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw CustomException.badRequest("Only paid payments can be refunded");
        }

        Stripe.apiKey = stripeApiKey;

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment.getStripePaymentId());

            // Create refund
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", paymentIntent.getId());

            // Uncomment to process refund
            // Refund refund = Refund.create(params);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setPaytmentDate(LocalDateTime.now());

            // Update order payment status
            Order order = payment.getOrder();
            orderService.updatePaymentStatus(order.getId(), PaymentStatus.REFUNDED);

            Payment updatedPayment = paymentRepository.save(payment);
            log.info("Payment refunded: {}", payment.getStripePaymentId());

            return convertToResponse(updatedPayment);

        } catch (StripeException e) {
            log.error("Refund failed: {}", e.getMessage(), e);
            throw CustomException.internalServerError("Refund processing failed: " + e.getMessage());
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        // Implement webhook handling for Stripe events
        // This should verify the webhook signature and process events
        log.info("Webhook received: {}", payload);

        // Parse the event and update payment status accordingly
        // This is a simplified version - in production, you should verify the webhook signature
        try {
            // Parse the event
            // Update payment status based on event type
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            throw CustomException.internalServerError("Webhook processing failed");
        }
    }

    private PaymentResponse convertToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .stripePaymentId(payment.getStripePaymentId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .paymentDate(payment.getPaytmentDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}