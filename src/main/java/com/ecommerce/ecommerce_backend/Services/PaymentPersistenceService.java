package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.Repositories.PaymentRepository;
import com.ecommerce.ecommerce_backend.model.Order;
import com.ecommerce.ecommerce_backend.model.Payment;
import com.ecommerce.ecommerce_backend.model.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentPersistenceService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment savePendingPayment(
            Order order,
            String transactionId,
            BigDecimal amount
    ) {
        String cleanTransactionId =
                transactionId == null ? null : transactionId.trim();

        Payment payment = paymentRepository
                .findByOrderId(order.getId())
                .orElseGet(() -> Payment.builder()
                        .order(order)
                        .build());

        payment.setTransactionId(cleanTransactionId);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod("PAYWAY_QR");

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        log.info(
                "Saved PayWay transaction: [{}], paymentId={}, orderId={}, length={}",
                savedPayment.getTransactionId(),
                savedPayment.getId(),
                order.getId(),
                savedPayment.getTransactionId() == null
                        ? 0
                        : savedPayment.getTransactionId().length()
        );

        return savedPayment;
    }
}
