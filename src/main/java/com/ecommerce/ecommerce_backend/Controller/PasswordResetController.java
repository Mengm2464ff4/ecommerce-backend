package com.ecommerce.ecommerce_backend.Controller;


import com.ecommerce.ecommerce_backend.dto.request.ForgotPasswordRequest;
import com.ecommerce.ecommerce_backend.dto.response.MessageResponse;
import com.ecommerce.ecommerce_backend.dto.request.ResetPasswordRequest;
import com.ecommerce.ecommerce_backend.Services.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.requestReset(request);

        // 🔐 Always the same message → prevents email enumeration
        return ResponseEntity.ok(new MessageResponse(
                "If that email exists, we've sent a reset link."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password updated successfully."));
    }
}
