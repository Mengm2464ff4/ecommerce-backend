package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Services.InvoiceService; // Import your InvoiceService
import com.ecommerce.ecommerce_backend.Services.OrderService;
import com.ecommerce.ecommerce_backend.Services.PayWayService;
import com.ecommerce.ecommerce_backend.dto.request.OrderRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.OrderResponse;
import com.ecommerce.ecommerce_backend.model.OrderStatus;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
//    private final PayWayService payWayService;
    private final InvoiceService invoiceService; // Added InvoiceService dependency

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Creating new order for user: {}", username);
        OrderResponse order = orderService.createOrder(username, orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String username = authentication.getName();
        log.info("Fetching orders for user: {}", username);
        Page<OrderResponse> orders = orderService.getUserOrders(username, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetails(
            @PathVariable Long orderId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Fetching order details for orderId: {} by user: {}", orderId, username);
        OrderResponse order = orderService.getOrderDetails(username, orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order details retrieved successfully"));
    }

    // ================= DOWNLOAD INVOICE ENDPOINT =================
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<Resource> downloadInvoice(
            @PathVariable Long orderId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("User {} requesting invoice for orderId: {}", username, orderId);

        // Security check: Ensures the user owns this order before downloading
        orderService.getOrderDetails(username, orderId);

        ByteArrayResource pdfResource = invoiceService.generateInvoicePdf(orderId);

        byte[] pdfBytes = pdfResource.getByteArray(); // or however you get the bytes

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(new ByteArrayResource(pdfBytes));
    }

    @PostMapping("/{orderId}/pay") // 👈 Change it to this
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePayWayPayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String username = authentication.getName();

        log.info(
                "Creating KHQR payment for orderId: {} by user: {}",
                orderId,
                username
        );

        // Note: make sure your OrderService actually has this method name
        Map<String, Object> response =
                orderService.createPayWayPayment(username, orderId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "QR generated successfully"
                )
        );
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String username = authentication.getName();
        OrderResponse order = orderService.cancelUserOrder(username, orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled"));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Fetching order by number: {} for user: {}", orderNumber, username);
        OrderResponse order = orderService.getOrderByNumber(username, orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderStatus>> trackOrder(@PathVariable String orderNumber) {
        log.info("Tracking order: {}", orderNumber);
        OrderStatus status = orderService.getOrderStatus(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(status, "Order status retrieved successfully"));
    }

    // Admin: Update order status
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        log.info("Updating order status for orderId: {} to {}", orderId, status);
        OrderResponse order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
    }

    // Admin: Get all orders
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 10, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching all orders for admin");
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "All orders retrieved successfully"));
    }

    // Admin: Get orders by status
    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @PageableDefault(size = 10, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching orders by status: {}", status);
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders by status retrieved successfully"));
    }

    // Admin: Cancel order
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long orderId) {
        log.info("Cancelling order with id: {}", orderId);
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Order cancelled successfully"));
    }
}