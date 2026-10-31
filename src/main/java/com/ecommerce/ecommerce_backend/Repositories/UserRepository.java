package com.ecommerce.ecommerce_backend.Repositories;


import com.ecommerce.ecommerce_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByUsername (String username);
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByEmail (String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}
