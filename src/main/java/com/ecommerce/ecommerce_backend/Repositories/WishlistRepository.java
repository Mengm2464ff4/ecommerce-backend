package com.ecommerce.ecommerce_backend.Repositories;

import com.ecommerce.ecommerce_backend.model.Wishlist;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUser(User user);
    Optional<Wishlist> findByUserAndProduct(User user, Product product);

    // 👈 Change this line:
    long countByUserId(Long userId);

    void deleteByUserAndProduct(User user, Product product);
}