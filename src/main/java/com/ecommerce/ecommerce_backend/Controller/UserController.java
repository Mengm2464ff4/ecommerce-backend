package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Repositories.WishlistRepository;
import com.ecommerce.ecommerce_backend.Services.UserService;
import com.ecommerce.ecommerce_backend.dto.request.UpdateProfileRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.UserResponse;
import com.ecommerce.ecommerce_backend.model.Role;
// Add these imports
import com.ecommerce.ecommerce_backend.Repositories.OrderRepository;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.Repositories.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map; // Add this to your imports at the top
import java.util.HashMap; // Add this to your imports at the top

// ... (inside your UserController class, below getCurrentUser) ...
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    // ADD THESE TWO REPOSITORIES HERE:
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching current user: {}", username);
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(user, "User details retrieved successfully."));
    }

    // ✅ FIXED: added leading slash
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String username = authentication.getName();
        log.info("Updating profile for user: {}", username);
        UserResponse updatedUser = userService.updateProfile(username, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully."));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(Authentication authentication) {
        try {
            String identifier = authentication.getName();

            // 1. Find user by email OR username
            User user = userRepository.findByEmail(identifier)
                    .orElseGet(() -> userRepository.findByUsername(identifier)
                            .orElseThrow(() -> new RuntimeException("User not found: " + identifier)));

            // 2. Count orders and wishlist safely
            long realOrderCount = orderRepository.countByUserId(user.getId());
            long realWishlistCount = wishlistRepository.countByUserId(user.getId());

            Map<String, Object> stats = new HashMap<>();
            stats.put("orders", realOrderCount);
            stats.put("wishlist", realWishlistCount);
            stats.put("memberSince", user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : "Recently");
            stats.put("rewardPoints", 0);

            return ResponseEntity.ok(ApiResponse.success(stats, "User stats retrieved successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching stats: " + e.getMessage(), 500));
        }
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("Fetching user by id: {}", id);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user by username: {}", username);
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        log.info("Fetching user by email: {}", email);
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("Fetching all users with pagination");
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsersList() {
        log.info("Fetching all users as list");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteUser(@PathVariable Long id) {
        log.info("Permanently deleting user with id: {}", id);
        userService.permanentlyDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User permanently deleted successfully"));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        log.info("Activating user with id: {}", id);
        userService.activateUser(id);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User activated successfully"));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        log.info("Deactivating user with id: {}", id);
        userService.deleteUser(id);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User deactivated successfully"));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable Long id,
            @RequestParam Role role
    ) {
        log.info("Changing role for user: {} to: {}", id, role);
        userService.changeUserRole(id, role);
        UserResponse user = userService.changeUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success(user, "User role updated successfully"));
    }

    @GetMapping("/exists/username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameExists(@RequestParam String username) {
        log.info("Checking if username exists: {}", username);
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(exists, "Username existence checked"));
    }

    @GetMapping("/exists/email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@RequestParam String email) {
        log.info("Checking if email exists: {}", email);
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(exists, "Email existence checked"));
    }

    @GetMapping("/make-me-admin")
    public ResponseEntity<String> makeMeAdmin(Authentication authentication) {
        try {
            String identifier = authentication.getName();

            // 👈 FIX: Look for user by EMAIL or USERNAME
            User user = userRepository.findByEmail(identifier)
                    .orElseGet(() -> userRepository.findByUsername(identifier)
                            .orElseThrow(() -> new RuntimeException("User not found: " + identifier)));

            user.setRole(Role.ADMIN);
            user.setActive(true);
            user.setEmailVerified(true);
            userRepository.saveAndFlush(user);

            return ResponseEntity.ok("SUCCESS! You are now an ADMIN. Log out and log back in.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }
}