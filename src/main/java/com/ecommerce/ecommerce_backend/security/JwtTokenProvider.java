package com.ecommerce.ecommerce_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationInMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private int refreshExpirationInMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes();
        if (keyBytes.length < 64) {
            byte[] paddedKey = new byte[64];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 64));
            signingKey = Keys.hmacShaKeyFor(paddedKey);
        } else {
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
        log.info("JWT Token Provider initialized");
    }

    public String generateToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        String username;

        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String principalString) {
            username = principalString;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported authentication principal type: "
                            + principal.getClass().getName()
            );
        }

        return generateToken(username);
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username != null
                    && username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    public Date getTokenExpirationDate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error getting token expiration: {}", e.getMessage());
            return null;
        }
    }

    public long getTokenExpirationInMs() {
        return jwtExpirationInMs;
    }

    public long getRefreshExpirationInMs() {
        return refreshExpirationInMs;
    }

    public String refreshToken(String token) {
        try {
            String username = getUsernameFromToken(token);
            if (username != null && !isTokenExpired(token)) {
                Date now = new Date();
                Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

                return Jwts.builder()
                        .subject(username)
                        .issuedAt(now)
                        .expiration(expiryDate)
                        .signWith(signingKey)
                        .compact();
            }
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
        }
        return null;
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            return null;
        }
    }

    public String generateTokenWithClaims(String username, Claims claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey);

        if (claims != null) {
            builder.claims(claims);
        }

        return builder.compact();
    }
}