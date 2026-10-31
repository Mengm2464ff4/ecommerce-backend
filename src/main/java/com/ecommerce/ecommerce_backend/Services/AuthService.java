// src/main/java/com/ecommerce/service/AuthService.java
package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.LoginRequest;
import com.ecommerce.ecommerce_backend.dto.request.RegisterRequest;
import com.ecommerce.ecommerce_backend.dto.response.AuthResponse;
import com.ecommerce.ecommerce_backend.exception.CustomException;
import com.ecommerce.ecommerce_backend.model.Role;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.Repositories.UserRepository;
import com.ecommerce.ecommerce_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TelegramService telegramService;

    // 👇 Anti-spam map for failed login alerts
    private final Map<String, Long> lastLoginAlert = new ConcurrentHashMap<>();

//    @Transactional
//    public AuthResponse login(LoginRequest loginRequest) {
//        try {
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            loginRequest.getEmail(),
//                            loginRequest.getPassword()
//                    )
//            );
//
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//            String jwt = tokenProvider.generateToken(authentication);
//
//        User user = userRepository.findByUsername(loginRequest.getEmail())
//               .orElseThrow(() -> CustomException.notFound("User not found"));
// To this:
//            User user = userRepository.findByEmail(loginRequest.getEmail())
//                    .orElseThrow(() -> CustomException.notFound("User not found"));
//
//            log.info("User logged in successfully: {}", user.getUsername());
//
//            return AuthResponse.builder()
//                    .token(jwt)
//                    .type("Bearer")
//                    .id(user.getId())
//                    .username(user.getUsername())
//                    .email(user.getEmail())
//                    .role(user.getRole().name())
//                    .firstName(user.getFirstName())
//                    .lastName(user.getLastName())
//                    .phoneNumber(user.getPhoneNumber())
//                    .shippingAddress(user.getShippingAddress())
//                    .isActive(user.isActive())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Login failed for user: {}", loginRequest.getEmail(), e);
//
//            // 👇 FAILED LOGIN TELEGRAM ALERT (rate-limited: once per 5 min per username)
//            String key = loginRequest.getEmail();
//            long now = System.currentTimeMillis();
//            Long last = lastLoginAlert.get(key);
//
//            if (last == null || now - last > 5 * 60 * 1000) {
//                lastLoginAlert.put(key, now);
//                telegramService.sendMessage(
//                        "⚠️ <b>FAILED LOGIN ATTEMPT</b>\n" +
//                                "━━━━━━━━━━━━━━━━━━━━\n\n" +
//                                "👤 <b>Username:</b> " + loginRequest.getEmail() + "\n" +
//                                "🚫 <b>Reason:</b> Invalid credentials\n\n" +
//                                "━━━━━━━━━━━━━━━━━━━━\n" +
//                                "🕐 <b>Time:</b> " + LocalDateTime.now().format(
//                                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
//                        )
//                );
//            }
//
//            throw CustomException.unauthorized("Invalid username or password");
//        }
//    }

//    @Transactional
//    public String register(RegisterRequest registerRequest) {
//        // Check if username exists
//        if (userRepository.existsByUsername(registerRequest.getUsername())) {
//            throw CustomException.conflict("Username is already taken!");
//        }
//
//        // Check if email exists
//        if (userRepository.existsByEmail(registerRequest.getEmail())) {
//            throw CustomException.conflict("Email is already in use!");
//        }
//
//        // Create new user
//        User user = User.builder()
//                .username(registerRequest.getUsername())
//                .email(registerRequest.getEmail())
//                .password(passwordEncoder.encode(registerRequest.getPassword()))
//                .firstName(registerRequest.getFirstName())
//                .lastName(registerRequest.getLastName())
//                .role(Role.USER)
//                .isActive(true)
//                .emailVerified(true) // 👈 ADD THIS LINE
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        userRepository.save(user);
//        log.info("User registered successfully: {}", user.getUsername());
//
//        // 👇 NEW USER TELEGRAM NOTIFICATION
//        telegramService.sendMessage(
//                "🛍️ <b>NEW CUSTOMER REGISTERED</b>\n" +
//                        "━━━━━━━━━━━━━━━━━━━━\n\n" +
//                        "👤 <b>Username:</b> " + user.getUsername() + "\n" +
//                        "📧 <b>Email:</b> " + user.getEmail() + "\n" +
//                        "📝 <b>Name:</b> " + user.getFirstName() + " " + user.getLastName() + "\n" +
//                        "🎭 <b>Role:</b> " + user.getRole().name() + "\n" +
//                        "✅ <b>Status:</b> Active\n\n" +
//                        "━━━━━━━━━━━━━━━━━━━━\n" +
//                        "🕐 <b>Time:</b> " + LocalDateTime.now().format(
//                        java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
//                ) + "\n" +
//                        "🆔 <b>User ID:</b> #" + user.getId()
//        );
//
//        return "User registered successfully!";
//    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            System.out.println("=== LOGIN START ===");
            System.out.println("Email: " + loginRequest.getEmail());
            System.out.println("Password: " + loginRequest.getPassword());

            // 1. Find user
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequest.getEmail()));
            System.out.println("=== USER FOUND: " + user.getEmail() + " ===");

            // 2. Check password
            boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
            System.out.println("=== PASSWORD MATCHES: " + passwordMatches + " ===");
            System.out.println("=== DB HASH: " + user.getPassword() + " ===");

            if (!passwordMatches) {
                throw new RuntimeException("Wrong password!");
            }

            // 3. Create Authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getEmail(),
                    null,
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            System.out.println("=== AUTH CREATED ===");

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 4. Generate Token
            System.out.println("=== GENERATING TOKEN ===");
            String jwt = tokenProvider.generateToken(authentication);
            System.out.println("=== TOKEN GENERATED: " + jwt.substring(0, 20) + "... ===");

            log.info("User logged in successfully: {}", user.getEmail());

            AuthResponse response = AuthResponse.builder()
                    .token(jwt)
                    .type("Bearer")
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .shippingAddress(user.getShippingAddress())
                    .isActive(user.isActive())
                    .build();

            System.out.println("=== LOGIN SUCCESS ===");
            return response;

        } catch (Exception e) {
            System.out.println("=== LOGIN FAILED: " + e.getMessage() + " ===");
            e.printStackTrace();
            log.error("Login failed for user: {}", loginRequest.getEmail(), e);
            throw CustomException.unauthorized(e.getMessage());
        }
    }

    @Transactional
    public String register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw CustomException.conflict("Email is already in use!");
        }

        // 👇 Auto-generate username from email if not provided
        String username = registerRequest.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = registerRequest.getEmail().split("@")[0]; // "test9999" from "test9999@gmail.com"
        }

        User user = User.builder()
                .username(username) // 👈 Use the auto-generated username
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {} (username: {})", user.getEmail(), user.getUsername());
        return "User registered successfully!";
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> CustomException.notFound("User not found"));
    }

    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("User logged out");
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    public String getUsernameFromToken(String token) {
        return tokenProvider.getUsernameFromToken(token);
    }
}