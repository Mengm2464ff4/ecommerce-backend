package com.ecommerce.ecommerce_backend.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        try {
            String verifyUrl = frontendUrl + "/verify-email?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verify your email address");

            String html = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:30px;'>"
                    + "<h2 style='color:#2563eb;'>Welcome to our store!</h2>"
                    + "<p>Hi " + firstName + ",</p>"
                    + "<p>Thanks for signing up. Please verify your email address by clicking the button below:</p>"
                    + "<p style='text-align:center;margin:30px 0;'>"
                    + "<a href='" + verifyUrl + "' "
                    + "style='background:#2563eb;color:white;padding:14px 28px;text-decoration:none;"
                    + "border-radius:8px;font-weight:bold;display:inline-block;'>Verify Email</a>"
                    + "</p>"
                    + "<p>Or copy this link into your browser:</p>"
                    + "<p style='word-break:break-all;color:#666;'>" + verifyUrl + "</p>"
                    + "<p style='color:#999;font-size:12px;margin-top:30px;'>"
                    + "This link expires in 24 hours. If you didn't create an account, ignore this email.</p>"
                    + "</div>";

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }
}