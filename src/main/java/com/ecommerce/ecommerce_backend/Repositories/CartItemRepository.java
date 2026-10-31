package com.ecommerce.ecommerce_backend.Repositories;

import com.ecommerce.ecommerce_backend.model.Cart;
import com.ecommerce.ecommerce_backend.model.CartItem;
import com.ecommerce.ecommerce_backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Find all items in a cart
    List<CartItem> findByCart(Cart cart);

    // Find all items by cart ID
    List<CartItem> findByCartId(Long cartId);

    // Find specific item by cart + product
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // Find specific item by cart ID + product ID
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    // 👇 NEW — size-aware lookups
    Optional<CartItem> findByCartAndProductAndSize(Cart cart, Product product, String size);

    // Delete all items in a cart
    void deleteByCart(Cart cart);

    // Delete all items by cart ID
    void deleteByCartId(Long cartId);

    // Delete specific item by cart + product
    void deleteByCartAndProduct(Cart cart, Product product);

    // Count items in a cart
    long countByCart(Cart cart);
}