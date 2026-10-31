package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.CartRequest;
import com.ecommerce.ecommerce_backend.dto.response.CartResponse;
import com.ecommerce.ecommerce_backend.exception.CustomException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.model.Cart;
import com.ecommerce.ecommerce_backend.model.CartItem;
import com.ecommerce.ecommerce_backend.model.Product;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.Repositories.CartRepository;
import com.ecommerce.ecommerce_backend.Repositories.CartItemRepository;
import com.ecommerce.ecommerce_backend.Repositories.ProductRepository;
import com.ecommerce.ecommerce_backend.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartResponse getCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Cart cart = getOrCreateCart(user);
        return convertToResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(String username, CartRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (product.getStock() < request.getQuantity()) {
            throw CustomException.badRequest(
                    "Insufficient stock. Available: " + product.getStock() +
                            ", Requested: " + request.getQuantity());
        }

        Cart cart = getOrCreateCart(user);
        String size = request.getSize();

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId())
                        && Objects.equals(i.getSize(), size))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            if (product.getStock() < newQuantity) {
                throw CustomException.badRequest(
                        "Insufficient stock. Available: " + product.getStock() +
                                ", Requested total: " + newQuantity);
            }

            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
            log.info("Updated cart item: {} (size {}) quantity to {}",
                    product.getName(), size, newQuantity);
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .size(size)
                    .build();
            cartItemRepository.save(cartItem);
            cart.getItems().add(cartItem);                              // ← FIX
            log.info("Added new item to cart: {} (size {})", product.getName(), size);
        }

        return convertToResponse(cart);                                 // ← FIX (no re-fetch)
    }

    @Transactional
    public CartResponse updateCartItemQuantity(String username, Long itemId, Integer quantity) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (quantity <= 0) {
            return removeFromCart(username, itemId);
        }

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw CustomException.unauthorized("Not your cart item");
        }

        Product product = cartItem.getProduct();
        if (product.getStock() < quantity) {
            throw CustomException.badRequest(
                    "Insufficient stock. Available: " + product.getStock() +
                            ", Requested: " + quantity);
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        log.info("Updated cart item {} quantity -> {}", itemId, quantity);

        return convertToResponse(cartItem.getCart());                   // ← FIX (no re-fetch)
    }

    @Transactional
    public CartResponse removeFromCart(String username, Long itemId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw CustomException.unauthorized("Not your cart item");
        }

        Cart parentCart = cartItem.getCart();
        parentCart.getItems().remove(cartItem);                         // ← FIX (sync in-memory)
        cartItemRepository.delete(cartItem);
        log.info("Removed cart item {} for user {}", itemId, username);

        return convertToResponse(parentCart);                           // ← FIX (no re-fetch)
    }

    @Transactional
    public void clearCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCart(cart);
        log.info("Cleared cart for user: {}", username);
    }

    @Transactional
    public CartResponse syncCart(String username, List<CartRequest> cartItems) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCart(cart);
        cart.getItems().clear();                                        // ← FIX (sync in-memory)

        if (cartItems != null) {
            for (CartRequest request : cartItems) {
                Product product = productRepository.findById(request.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

                if (product.getStock() < request.getQuantity()) {
                    throw CustomException.badRequest(
                            "Insufficient stock for product " + product.getName());
                }

                CartItem cartItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(request.getQuantity())
                        .price(product.getPrice())
                        .size(request.getSize())
                        .build();

                cartItemRepository.save(cartItem);
                cart.getItems().add(cartItem);                          // ← FIX
            }
        }

        log.info("Synced cart for user: {} with {} items", username,
                cartItems != null ? cartItems.size() : 0);
        return convertToResponse(cart);
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse convertToResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            BigDecimal subtotal = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            CartResponse.CartItemResponse itemResponse = CartResponse.CartItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .productImageUrl(item.getProduct().getImageUrl())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .size(item.getSize())
                    .subtotal(subtotal)
                    .build();

            itemResponses.add(itemResponse);
        }

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .username(cart.getUser().getUsername())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItems(itemResponses.size())
                .build();
    }

    public int getCartItemCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null) return 0;
        return (int) cartItemRepository.countByCart(cart);
    }

    public BigDecimal getCartTotal(String username) {
        CartResponse cart = getCart(username);
        return cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO;
    }
}