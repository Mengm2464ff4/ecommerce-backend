package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Repositories.PaymentRepository;
import com.ecommerce.ecommerce_backend.Services.OrderService;
import com.ecommerce.ecommerce_backend.Services.PayWayService;
import com.ecommerce.ecommerce_backend.Services.TelegramService;
import com.ecommerce.ecommerce_backend.model.Payment;
import com.ecommerce.ecommerce_backend.model.PaymentStatus;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/payway")
@RequiredArgsConstructor
@Slf4j
public class PayWayController {

    private final PayWayService payWayService;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final TelegramService telegramService;

    @Transactional
    @PostMapping("/callback")
    public ResponseEntity<String> handlePayWayCallback(
            @RequestBody Map<String, Object> payload
    ) {
        String receivedHash = getValue(payload, "hash");
        String tranId = getValue(payload, "tran_id");
        String status = getValue(payload, "status");

        log.info(
                "PayWay callback received: tranId={}, status={}",
                tranId,
                status
        );

        if (tranId != null) {
            tranId = tranId.trim();
        }


        if (tranId == null || tranId.isBlank()) {
            log.error("PayWay callback is missing tran_id");
            return ResponseEntity.badRequest()
                    .body("Missing tran_id");
        }

        if (receivedHash == null || receivedHash.isBlank()) {
            log.error(
                    "PayWay callback is missing hash. tranId={}",
                    tranId
            );

            return ResponseEntity.badRequest()
                    .body("Missing hash");
        }

        if (status == null || status.isBlank()) {
            log.error(
                    "PayWay callback is missing status. tranId={}",
                    tranId
            );

            return ResponseEntity.badRequest()
                    .body("Missing status");
        }

        /*
         * Verify the callback signature before updating anything.
         */
        boolean hashValid;

        try {
            hashValid = payWayService.verifyCallbackHash(
                    payload,
                    receivedHash
            );
        } catch (Exception exception) {
            log.error(
                    "PayWay hash verification failed. tranId={}",
                    tranId,
                    exception
            );

            return ResponseEntity.status(401)
                    .body("Hash verification failed");
        }

        if (!hashValid) {
            log.error(
                    "Invalid PayWay callback hash. tranId={}",
                    tranId
            );

            return ResponseEntity.status(401)
                    .body("Invalid hash");
        }

        /*
         * PayWay status 0 means successful payment.
         */
        if (!"0".equals(status)) {
            log.warn(
                    "PayWay payment was not successful. tranId={}, status={}",
                    tranId,
                    status
            );

            return ResponseEntity.ok("Callback received");
        }

        /*
         * Find the payment using the exact transaction ID that was saved
         * before the QR request was sent.
         */
        log.info(
                "Looking up PayWay transaction: [{}], length={}",
                tranId,
                tranId == null ? 0 : tranId.length()
        );
        Payment payment = paymentRepository
                .findByTransactionId(tranId)
                .orElse(null);

        if (payment == null) {
            log.error(
                    "Payment not found for PayWay transaction. tranId={}",
                    tranId
            );

            /*
             * Returning 404 may cause PayWay to retry the callback.
             * Return 200 only if you do not want automatic retries.
             */
            return ResponseEntity.status(404)
                    .body("Transaction not found");
        }

        /*
         * Ignore duplicate callbacks.
         */
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info(
                    "Duplicate callback ignored. tranId={}",
                    tranId
            );

            return ResponseEntity.ok(
                    "Payment already processed"
            );
        }

        /*
         * Important: your original code had setPaytmentDate().
         * The correct method should normally be setPaymentDate().
         */
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaytmentDate(LocalDateTime.now());

        paymentRepository.save(payment);

        /*
         * This requires Payment.java to contain:
         *
         * @ManyToOne
         * private Order order;
         */
        if (payment.getOrder() == null) {
            log.error(
                    "Payment has no related order. paymentId={}, tranId={}",
                    payment.getId(),
                    tranId
            );

            return ResponseEntity.internalServerError()
                    .body("Payment has no related order");
        }

        Long orderId = payment.getOrder().getId();

        /*
         * Mark the related order as paid.
         */
        orderService.markOrderAsPaid(orderId);

        log.info(
                "Payment confirmed. paymentId={}, orderId={}, tranId={}",
                payment.getId(),
                orderId,
                tranId
        );

        /*
         * Telegram notification must not undo a successful payment.
         */
        try {
            String paymentTime = LocalDateTime.now()
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy, hh:mm a"
                            )
                    );

            telegramService.sendMessage(
                    "💰 <b>PAYMENT RECEIVED</b>\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n\n" +
                            "📦 <b>Order ID:</b> " + orderId + "\n" +
                            "🔗 <b>PayWay Transaction:</b> " + tranId + "\n" +
                            "✅ <b>Status:</b> PAID\n" +
                            "💵 <b>Method:</b> ABA PayWay QR\n\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "🕐 <b>Time:</b> " + paymentTime
            );
        } catch (Exception exception) {
            log.error(
                    "Telegram notification failed after successful payment. " +
                            "tranId={}, orderId={}",
                    tranId,
                    orderId,
                    exception
            );
        }

        return ResponseEntity.ok(
                "Payment processed successfully"
        );
    }

    private String getValue(
            Map<String, Object> payload,
            String key
    ) {
        if (payload == null) {
            return null;
        }

        Object value = payload.get(key);

        return value == null
                ? null
                : String.valueOf(value);
    }
}
