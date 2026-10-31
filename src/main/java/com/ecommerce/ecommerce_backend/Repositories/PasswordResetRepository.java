package com.ecommerce.ecommerce_backend.Repositories;

import com.ecommerce.ecommerce_backend.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByTokenHashAndUsedFalse(String tokenHash);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordReset p WHERE p.userId = :userId")
    void deleteAllByUserId(Long userId);
}