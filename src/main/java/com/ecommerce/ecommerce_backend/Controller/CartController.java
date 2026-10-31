package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Services.CartService;
import com.ecommerce.ecommerce_backend.dto.request.CartRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.CartResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching cart for user: {}", username);
        CartResponse cart = cartService.getCart(username);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart retrieved successfully."));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching cart item count for user: {}", username);
        int count = cartService.getCartItemCount(username);
        return ResponseEntity.ok(ApiResponse.success(count, "Cart count retrieved successfully."));
    }

    @GetMapping("/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getCartTotal(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching cart total for user: {}", username);
        BigDecimal total = cartService.getCartTotal(username);
        return ResponseEntity.ok(ApiResponse.success(total, "Cart total retrieved successfully."));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartRequest cartRequest,
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("Adding product {} to cart for user: {}", cartRequest.getProductId(), username);
        CartResponse cart = cartService.addToCart(username, cartRequest);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart successfully."));
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(
            @PathVariable Long itemId,
            @RequestParam Integer quantity,
            Authentication authentication
    ) {
        String username = authentication.getName();
        CartResponse cart = cartService.updateCartItemQuantity(username, itemId, quantity);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart updated successfully."));
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        CartResponse cart = cartService.removeFromCart(username, itemId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart successfully."));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        String username = authentication.getName();
        log.info("Clearing cart for user: {}", username);
        cartService.clearCart(username);
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared successfully."));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<CartResponse>> syncCart(
            @RequestBody List<CartRequest> cartItems,
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("Syncing cart for user: {} with {} items", username, cartItems != null ? cartItems.size() : 0);
        CartResponse cart = cartService.syncCart(username, cartItems);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart synced successfully."));
    }
}