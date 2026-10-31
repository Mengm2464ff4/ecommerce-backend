package com.ecommerce.ecommerce_backend.security;


import com.ecommerce.ecommerce_backend.Repositories.UserRepository;
import com.ecommerce.ecommerce_backend.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {


    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        log.debug("Loading user by identifier: {}", identifier);

        // Try email first, then username
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByUsername(identifier)
                        .orElseThrow(() -> {
                            log.error("User not found: {}", identifier);
                            return new UsernameNotFoundException("User not found: " + identifier);
                        }));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is disabled: " + identifier);
        }

        log.debug("User found: {} (email: {})", user.getUsername(), user.getEmail());
        return user;
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email; {}", email);
                    return new UsernameNotFoundException("User not found with email:" + email);
                });

        if (!user.isActive()) {
            log.error("User account is disable: {}", email);
            throw new UsernameNotFoundException("User account is disable:" + email);
        }

        log.debug("User found by email: {}", user.getEmail());
        return user;
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById (Long id) throws UsernameNotFoundException {
        log.debug("Loading user by id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                   log.error("User not found with id: {}", id);
                   return new UsernameNotFoundException("User not found with id:" + id);
                });
        if (!user.isActive()) {
            log.error("User account is disabled with id: {}", id);
            throw new UsernameNotFoundException("User account is disabled with id:" + id);
        }
        log.debug("User found by id: {}", user.getId());
        return user;
    }

    // Additional helper methods
    public boolean isUserActive(String username) {
        return userRepository.findByUsername(username)
                .map(User::isActive)
                .orElse(false);
    }

    public boolean userExists (String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    //Method to get user with authorities
    private List<SimpleGrantedAuthority> getAuthorities (User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    // Method to create UserDetails object manually (if needed)
    public UserDetails createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                true,
                true,
                true,
                getAuthorities(user)
        );
    }

    // Get user entity (not just UserDetails)
    public User hashRole (String username, String role) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username:" + username));

    }

    // Check if user has specific role
    public boolean hasRole(String username, String role) {
        return userRepository.findByUsername(username)
                .map(user -> user.getRole().name().equalsIgnoreCase(role))
                .orElse(false);
    }

    // Check if user is admin
    public boolean isAdmin(String username) {
        return hasRole(username, "ADMIN");
    }

    // Check if user is user
    public boolean isUser(String username) {
        return hasRole(username, "USER");
    }
}
