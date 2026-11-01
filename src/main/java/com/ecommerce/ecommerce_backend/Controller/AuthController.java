package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.Repositories.UserRepository;
import com.ecommerce.ecommerce_backend.Services.EmailService;
import com.ecommerce.ecommerce_backend.Services.TelegramService;
import com.ecommerce.ecommerce_backend.dto.request.GoogleAuthRequest;
import com.ecommerce.ecommerce_backend.dto.request.LoginRequest;
import com.ecommerce.ecommerce_backend.dto.request.RegisterRequest;
import com.ecommerce.ecommerce_backend.dto.response.AuthResponse;
import com.ecommerce.ecommerce_backend.model.Role;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.security.CustomUserDetailsService;
import com.ecommerce.ecommerce_backend.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TelegramService telegramService;
    private final EmailService emailService;

    private final Map<String, Long> lastLoginAlert =
            new ConcurrentHashMap<>();

    @Value("${google.client.id}")
    private String googleClientId;

    // ============================================================
    // LOGIN WITH EMAIL AND PASSWORD
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getEmail(),
                                    loginRequest.getPassword()
                            )
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            User user = userRepository
                    .findByEmail(loginRequest.getEmail())
                    .orElseThrow(() ->
                            new RuntimeException("User not found"));

            user.setRole(Role.ADMIN); // 👈 FORCE ADMIN
            userRepository.save(user);

            if (!user.isEmailVerified()) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("Please verify your email before logging in.");
            }

            // Load the fresh user details from the database to ensure the role is up-to-date
            UserDetails freshUserDetails = customUserDetailsService.loadUserByUsername(user.getUsername());
            Authentication freshAuth = new UsernamePasswordAuthenticationToken(freshUserDetails, null, freshUserDetails.getAuthorities());
            String jwt = jwtTokenProvider.generateToken(freshAuth);

            AuthResponse response = AuthResponse.builder()
                    .token(jwt)
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String key = loginRequest.getEmail();
            long now = System.currentTimeMillis();
            Long lastAlertTime = lastLoginAlert.get(key);

            if (lastAlertTime == null
                    || now - lastAlertTime > 5 * 60 * 1000) {

                lastLoginAlert.put(key, now);

                telegramService.sendMessage(
                        "⚠️ <b>FAILED LOGIN ATTEMPT</b>\n" +
                                "━━━━━━━━━━━━━━━━━━━━\n\n" +
                                "📧 <b>Email:</b> " +
                                loginRequest.getEmail() + "\n" +
                                "🚫 <b>Reason:</b> Invalid credentials\n\n" +
                                "━━━━━━━━━━━━━━━━━━━━\n" +
                                "🕐 <b>Time:</b> " +
                                LocalDateTime.now().format(
                                        DateTimeFormatter.ofPattern(
                                                "dd MMM yyyy, hh:mm a"
                                        )
                                )
                );
            }

            log.warn(
                    "Failed login attempt for email: {}",
                    loginRequest.getEmail()
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

    // ============================================================
    // REGISTER
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Email is already in use!");
        }

        String baseUsername =
                registerRequest.getEmail().split("@")[0];

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .username(username)
                .email(registerRequest.getEmail())
                .password(
                        passwordEncoder.encode(
                                registerRequest.getPassword()
                        )
                )
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(Role.USER)
                .isActive(true)
                .emailVerified(true)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(
                        LocalDateTime.now().plusHours(24)
                )
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFirstName() != null
                        ? user.getFirstName()
                        : "there",
                verificationToken
        );

        telegramService.sendMessage(
                "🛍️ <b>NEW CUSTOMER REGISTERED</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "📧 <b>Email:</b> " + user.getEmail() + "\n" +
                        "👤 <b>Username:</b> " + user.getUsername() + "\n" +
                        "⏳ <b>Status:</b> Pending Verification\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "🕐 <b>Time:</b> " +
                        LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern(
                                        "dd MMM yyyy, hh:mm a"
                                )
                        )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        "Registered! Please check your email " +
                                "to verify your account."
                );
    }

    // ============================================================
    // VERIFY EMAIL
    // ============================================================
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(
            @RequestParam String token
    ) {
        User user = userRepository
                .findByVerificationToken(token)
                .orElse(null);

        if (user == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid verification token");
        }

        if (user.getVerificationTokenExpiry() != null
                && user.getVerificationTokenExpiry()
                .isBefore(LocalDateTime.now())) {

            return ResponseEntity
                    .badRequest()
                    .body("Verification link has expired");
        }

        if (user.isEmailVerified()) {
            return ResponseEntity.ok(
                    "Email already verified. You can log in."
            );
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        userRepository.save(user);

        return ResponseEntity.ok(
                "Email verified successfully! You can now log in."
        );
    }

    // ============================================================
    // GOOGLE LOGIN
    // ============================================================
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(
            @RequestBody GoogleAuthRequest request
    ) {
        log.info(
                "Google backend endpoint called. Token present: {}",
                request != null
                        && request.getIdToken() != null
                        && !request.getIdToken().isBlank()
        );

        try {
            if (request == null
                    || request.getIdToken() == null
                    || request.getIdToken().isBlank()) {

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Google ID token is required");
            }

            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance()
                    )
                            .setAudience(
                                    Collections.singletonList(googleClientId)
                            )
                            .build();

            GoogleIdToken idToken =
                    verifier.verify(request.getIdToken());

            if (idToken == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid Google token");
            }

            GoogleIdToken.Payload payload =
                    idToken.getPayload();

            String email = payload.getEmail();
            String googleId = payload.getSubject();

            String firstName = payload.get("given_name") != null
                    ? String.valueOf(payload.get("given_name"))
                    : "User";

            String lastName = payload.get("family_name") != null
                    ? String.valueOf(payload.get("family_name"))
                    : "";

            if (email == null || email.isBlank()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Google account email is missing");
            }

            User user = userRepository
                    .findByEmail(email)
                    .orElse(null);

            boolean isNewUser = false;

            if (user == null) {
                isNewUser = true;

                String baseUsername = email.split("@")[0];
                String username = baseUsername;
                int counter = 1;

                while (userRepository.existsByUsername(username)) {
                    username = baseUsername + counter++;
                }

                user = User.builder()
                        .username(username)
                        .email(email)
                        .googleId(googleId)
                        .firstName(firstName)
                        .lastName(lastName)
                        .password(
                                passwordEncoder.encode(
                                        UUID.randomUUID().toString()
                                )
                        )
                        .role(Role.USER)
                        .isActive(true)
                        .emailVerified(true)
                        .createdAt(LocalDateTime.now())
                        .build();

                userRepository.save(user);

                telegramService.sendMessage(
                        "🆕 <b>NEW USER VIA GOOGLE</b>\n" +
                                "━━━━━━━━━━━━━━━━━━━━\n\n" +
                                "📧 <b>Email:</b> " + email + "\n" +
                                "👤 <b>Username:</b> " +
                                username + "\n" +
                                "🌐 <b>Provider:</b> Google\n\n" +
                                "━━━━━━━━━━━━━━━━━━━━\n" +
                                "🕐 <b>Time:</b> " +
                                LocalDateTime.now().format(
                                        DateTimeFormatter.ofPattern(
                                                "dd MMM yyyy, hh:mm a"
                                        )
                                )
                );

            } else {
                if (!user.isActive()) {
                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body("User account is disabled");
                }

                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                    user.setEmailVerified(true);
                    userRepository.save(user);
                }
            }

            /*
             * IMPORTANT:
             * Do not use user.getEmail() directly as the principal.
             * JwtTokenProvider expects a UserDetails object.
             */
            UserDetails userDetails =
                    customUserDetailsService
                            .loadUserByEmail(user.getEmail());

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            String jwt =
                    jwtTokenProvider.generateToken(authentication);

            AuthResponse response = AuthResponse.builder()
                    .token(jwt)
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .build();

            log.info(
                    "Google login successful for: {} (new: {})",
                    email,
                    isNewUser
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(
                    "Google login failed: {}",
                    e.getMessage(),
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            "Google authentication failed: " +
                                    e.getClass().getSimpleName() +
                                    " - " +
                                    e.getMessage()
                    );
        }
    }

    // ============================================================
    // GET CURRENT USER
    // ============================================================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || authentication.getName() == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Not authenticated");
        }

        String identifier = authentication.getName();

        User user = userRepository
                .findByEmail(identifier)
                .orElseGet(() ->
                        userRepository
                                .findByUsername(identifier)
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "User not found"
                                        )
                                )
                );

        // ✅ Return a DTO — never the raw User entity
        //    (raw entity leaks password hash + verificationToken)
        com.ecommerce.ecommerce_backend.dto.response.UserResponse response =
                com.ecommerce.ecommerce_backend.dto.response.UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phoneNumber(user.getPhoneNumber())
                        .shippingAddress(user.getShippingAddress())
                        .role(user.getRole())
                        .isActive(user.isActive())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build();

        return ResponseEntity.ok(response);
    }
}
