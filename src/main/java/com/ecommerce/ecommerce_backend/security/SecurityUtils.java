package com.ecommerce.ecommerce_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {

    private SecurityUtils() {
        //
    }
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String);
    }

    public static boolean hashRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    public static boolean isAdmin() {
        return hashRole("ADMIN");
    }

    public static boolean isUser() {
        return hashRole("USER");
    }

    public static String getCurrentUserIp() {
        //This would require HttpServletRequest injection
        //Implementation depends on your setup
        return null;
    }
}
