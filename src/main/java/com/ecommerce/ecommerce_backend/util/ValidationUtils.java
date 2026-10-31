package com.ecommerce.ecommerce_backend.util;


import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(AppConstants.EMAIL_PATTERN);
    private static final Pattern PHONE_PATTERN = Pattern.compile(AppConstants.PHONE_PATTERN);
    private static final Pattern USERNAME_PATTERN = Pattern.compile(AppConstants.USERNAME_PATTERN);

    private ValidationUtils () {
        throw  new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return StringUtils.hasText(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidUsername(String username) {
        return StringUtils.hasText(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return StringUtils.hasText(password) && password.length() >= 6;
    }

    public static boolean isNullOrEmpty(String value) {
        return !StringUtils.hasText(value);
    }

    public static boolean isNotNullAndNotEmpty(String value) {
        return StringUtils.hasText(value);
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    public static String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength) + "...";
    }
 }
