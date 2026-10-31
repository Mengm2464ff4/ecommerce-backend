// src/main/java/com/ecommerce/service/OrderService.java
package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.OrderRequest;
import com.ecommerce.ecommerce_backend.dto.response.OrderResponse;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.model.*;
import com.ecommerce.ecommerce_backend.Repositories.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final TelegramService telegramService;
    private final PaymentRepository paymentRepository;
    private final PaymentPersistenceService paymentPersistenceService;
    private final KhqrService khqrService; // 👈 Added our new KHQR Service

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ============================================================
    // CREATE KHQR PAYMENT (Replaces the old PayWay Method)
    // ============================================================
    @Transactional
    public Map<String, Object> createPayWayPayment(
            String username,
            Long orderId
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to pay for this order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot pay for a cancelled order");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("This order has already been paid");
        }

        // Generate a unique transaction ID (used for tracking in our database)
        String tranId = generateTransactionId(order.getId());

        log.info("Generated Transaction ID: {} (length={})", tranId, tranId.length());

        // Save a PENDING payment record in the database
        Payment savedPayment = paymentPersistenceService.savePendingPayment(
                order,
                tranId,
                order.getTotalAmount()
        );

        // 👇 CALL THE KHQR SERVICE HERE
        Map<String, Object> khqrResponse = khqrService.generateKHQR(
                order.getTotalAmount().doubleValue(),
                order.getOrderNumber()
        );

        // The KHQR response has data inside a "data" object
        Map<String, Object> responseData = (Map<String, Object>) khqrResponse.get("data");

        // Add our tracking IDs to the response so React knows what order this is
        responseData.put("transaction_id", savedPayment.getTransactionId());
        responseData.put("payment_id", savedPayment.getId());
        responseData.put("order_id", order.getId());

        log.info("KHQR Payment created: paymentId={}, orderId={}, tranId={}",
                savedPayment.getId(), order.getId(), tranId);

        return responseData;
    }


    @Transactional
    public OrderResponse createOrder(String username, OrderRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
            );

            // Update stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        Order order = Order.builder()
                .orderNumber("ORD" + (System.currentTimeMillis() % 10000000000000000L))
                .user(user)
                .items(orderItems)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(
                        request.getBillingAddress() != null
                                ? request.getBillingAddress()
                                : request.getShippingAddress()
                )
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .placedAt(LocalDateTime.now())
                .build();

        // Set order reference for each item
        orderItems.forEach(item -> item.setOrder(order));

        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order created: {} for user: {}", savedOrder.getOrderNumber(), username);

        // 👇 TELEGRAM NOTIFICATION
        telegramService.sendMessage(
                "🛒 <b>NEW ORDER PLACED</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "📦 <b>Order #:</b> " + savedOrder.getOrderNumber() + "\n" +
                        "👤 <b>Customer:</b> " + username + "\n" +
                        "💰 <b>Total:</b> $" + savedOrder.getTotalAmount() + "\n" +
                        "💳 <b>Payment:</b> " + savedOrder.getPaymentMethod() + "\n" +
                        "📊 <b>Status:</b> " + savedOrder.getStatus() + "\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "🕐 <b>Time:</b> " + LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                ) + "\n" +
                        "🆔 <b>Order ID:</b> #" + savedOrder.getId()
        );

        return convertToResponse(savedOrder);
    }

    // Helper method for Transaction ID
    private String generateTransactionId(Long orderId) {
        String orderPart = Long.toString(orderId, 36).toUpperCase();
        String timePart = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String tranId = "KHQR" + orderPart + timePart;
        return tranId.length() > 20 ? tranId.substring(0, 20) : tranId;
    }

    // ============================================================
    // GET USER ORDERS
    // ============================================================
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Order> orders = orderRepository.findByUserId(user.getId(), pageable);

        orders.getContent().forEach(order -> {
            if (order.getItems() != null) {
                order.getItems().forEach(item -> {
                    if (item.getProduct() != null) item.getProduct().getName();
                });
            }
        });

        return orders.map(this::convertToResponse);
    }

    // ============================================================
    // GET ORDER DETAILS
    // ============================================================
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(String username, Long orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to view this order");
        }

        return convertToResponse(order);
    }

    // ============================================================
    // GET ORDER BY NUMBER
    // ============================================================
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String username, String orderNumber) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to view this order");
        }

        return convertToResponse(order);
    }

    // ============================================================
    // CANCEL ORDER (USER)
    // ============================================================
    @Transactional
    public OrderResponse cancelUserOrder(String username, Long orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to cancel this order");
        }

        if (order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        log.info("Order cancelled by user {}: {}", username, saved.getOrderNumber());
        return convertToResponse(saved);
    }

    // ============================================================
    // GET ORDER STATUS
    // ============================================================
    @Transactional(readOnly = true)
    public OrderStatus getOrderStatus(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return order.getStatus();
    }

    // ============================================================
    // UPDATE ORDER STATUS (ADMIN)
    // ============================================================
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);

        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated: {} -> {}", updatedOrder.getOrderNumber(), status);
        return convertToResponse(updatedOrder);
    }

    // ============================================================
    // UPDATE PAYMENT STATUS
    // ============================================================
    @Transactional
    public void updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setPaymentStatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());

        if (paymentStatus == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.CONFIRMED);
        } else if (paymentStatus == PaymentStatus.REFUNDED) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);
        log.info("Order: {} payment status updated to: {}", order.getOrderNumber(), paymentStatus);
    }

    public void markOrderAsPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);

        orderRepository.save(order);
    }

    // ============================================================
    // GET ALL ORDERS (ADMIN)
    // ============================================================
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);

        orders.getContent().forEach(order -> {
            if (order.getUser() != null) order.getUser().getUsername();
            if (order.getItems() != null) {
                order.getItems().forEach(item -> {
                    if (item.getProduct() != null) item.getProduct().getName();
                });
            }
        });

        return orders.map(this::convertToResponse);
    }

    // ============================================================
    // GET ORDERS BY STATUS (ADMIN)
    // ============================================================
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatus(status, pageable);

        orders.getContent().forEach(order -> {
            if (order.getItems() != null) {
                order.getItems().forEach(item -> {
                    if (item.getProduct() != null) item.getProduct().getName();
                });
            }
        });

        return orders.map(this::convertToResponse);
    }

    // ============================================================
    // CANCEL ORDER (ADMIN)
    // ============================================================
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order cancelled: {}", order.getOrderNumber());
    }

    // ============================================================
    // CONVERT TO RESPONSE
    // ============================================================
    private OrderResponse convertToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemDtos = order.getItems() == null
                ? new ArrayList<>()
                : order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProduct() != null
                                ? item.getProduct().getName()
                                : "Unknown Product")
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .productImageUrl(item.getProduct() != null
                                ? item.getProduct().getImageUrl()
                                : null)
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .paymentMethod(order.getPaymentMethod())
                .placedAt(order.getPlacedAt())
                .deliveredAt(order.getDeliveredAt())
                .createdAt(order.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}