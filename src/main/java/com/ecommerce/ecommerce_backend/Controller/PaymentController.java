package com.ecommerce.ecommerce_backend.Controller;


import com.ecommerce.ecommerce_backend.Services.PaymentService;
import com.ecommerce.ecommerce_backend.dto.request.PaymentRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            Authentication authentication
            ) {
        String username = authentication.getName();
        log.info("Creating payment for order: {} by user: {}",paymentRequest.getOrderId(), username);
        PaymentResponse payment = paymentService.createPayment(paymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Paymeny created successfully."));
    }

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            Authentication authentication
    ) {
        String username  = authentication.getName();
        log.info("Processing payment for order: {] by user: {}", paymentRequest.getOrderId(), username);
        PaymentResponse payment = paymentService.processPayment(paymentRequest);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment processing successfully."));
    }

    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable("paymentIntentId") String paymentIntentId,
            Authentication authentication
    ) {
        String username = authentication.getName();

        log.info(
                "Confirming payment with intent ID: {} by user: {}",
                paymentIntentId,
                username
        );

        PaymentResponse payment =
                paymentService.confirmPayment(paymentIntentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        payment,
                        "Payment confirmed successfully."
                )
        );
    }


    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder (
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("Fetching payment for order: {} by user: {}", username);
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment retrived successfully."));
    }

    @GetMapping("/stripe/{stripePaymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByStripeId(
            @PathVariable String stripePaymentId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("fetching payment with stripe id: {} by user: {}", stripePaymentId, username);
        PaymentResponse payment = paymentService.getPaymentByStripeId(stripePaymentId);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment retrieved successfully"));
    }

    @PostMapping("/refund/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("Refunding payment: {} by admin: {}", paymentId, username);
        PaymentResponse payment = paymentService.refundPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment,"Payment refunded successfully"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        log.info("Received webhook from Stripe");
        paymentService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed successfully"));
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<ApiResponse<String>> getPaymentStatus(@PathVariable Long paymentId) {
        log.info("Checking payment status for payment: {}", paymentId);
        String status = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(ApiResponse.success(status, "Payment status retrieved successfully"));
    }

}
