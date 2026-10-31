package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Repositories.ProductRepository;
import com.ecommerce.ecommerce_backend.Repositories.UserRepository;
import com.ecommerce.ecommerce_backend.Repositories.WishlistRepository;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.WishlistItemResponse;
import com.ecommerce.ecommerce_backend.model.Product;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.model.Wishlist;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist(
            Authentication authentication
    ) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<WishlistItemResponse> items = wishlistRepository.findByUser(user)
                .stream()
                .map(WishlistItemResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(items, "Wishlist retrieved"));
    }

    @PostMapping("/{productId}")
    @Transactional // 👈 ADDED
    public ResponseEntity<ApiResponse<Void>> addToWishlist(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (wishlistRepository.findByUserAndProduct(user, product).isEmpty()) {
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(wishlist);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Added to wishlist"));
    }

    @DeleteMapping("/{productId}")
    @Transactional // 👈 ADDED
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 👈 CHANGED: Find the entity first, then delete it.
        // This prevents the bulk delete 500 error.
        wishlistRepository.findByUserAndProduct(user, product)
                .ifPresent(wishlistRepository::delete);

        return ResponseEntity.ok(ApiResponse.success(null, "Removed from wishlist"));
    }
}