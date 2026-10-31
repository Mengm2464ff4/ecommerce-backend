package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.ForgotPasswordRequest;
import com.ecommerce.ecommerce_backend.dto.request.ResetPasswordRequest;
import com.ecommerce.ecommerce_backend.model.PasswordReset;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.Repositories.PasswordResetRepository;
import com.ecommerce.ecommerce_backend.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;          // 256-bit token
    private static final int EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String mailFrom;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetRepository passwordResetRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Step 1: user requests reset link
     */
    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        // 🔐 SECURITY: always return normally, even if the email doesn't exist
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        // Remove old tokens for this user
        passwordResetRepository.deleteAllByUserId(user.getId());

        // Generate raw token, hash it for storage
        String rawToken = generateRawToken();
        String tokenHash = sha256Hex(rawToken);

        PasswordReset reset = new PasswordReset();
        reset.setUserId(user.getId());
        reset.setTokenHash(tokenHash);
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        reset.setUsed(false);
        passwordResetRepository.save(reset);

        // Build reset URL (frontend handles /reset-password?token=...)
        String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;

        sendResetEmail(user.getEmail(), resetUrl);
    }

    /**
     * Step 2: user submits new password + token
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = sha256Hex(request.getToken());

        PasswordReset reset = passwordResetRepository
                .findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        User user = userRepository.findById(reset.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used (single-use)
        reset.setUsed(true);
        passwordResetRepository.save(reset);
    }

    // ---------------- helpers ----------------

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private void sendResetEmail(String to, String resetUrl) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailFrom);
        msg.setTo(to);
        msg.setSubject("Reset your ShopHub password");
        msg.setText(
                "Hi,\n\n" +
                        "We received a request to reset your ShopHub password.\n\n" +
                        "Click the link below (expires in " + EXPIRY_MINUTES + " minutes):\n" +
                        resetUrl + "\n\n" +
                        "If you didn't request this, you can safely ignore this email.\n\n" +
                        "— The ShopHub Team"
        );
        mailSender.send(msg);
    }
}