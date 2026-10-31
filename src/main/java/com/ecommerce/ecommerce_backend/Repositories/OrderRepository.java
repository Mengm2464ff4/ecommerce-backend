package com.ecommerce.ecommerce_backend.Repositories;

import com.ecommerce.ecommerce_backend.model.Order;
import com.ecommerce.ecommerce_backend.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository <Order, Long>{

    Page<Order> findByUserId (Long userId, Pageable pageable);
    List<Order> findByUserId (Long userId);
    Optional<Order> findByOrderNumber( String orderNumber);
    Page<Order> findByStatus (OrderStatus status, Pageable pageable);
    // ✅ ADD THIS LINE:
    long countByUserId(Long userId);
}
