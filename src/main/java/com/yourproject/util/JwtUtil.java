package com.yourproject.util;

import com.yourproject.entity.Role; // Assuming Role enum is in this package
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultSecretKeyWhichIsVeryLongAndSecureAndAtLeast256Bits}") // Provide a strong default or ensure it's in properties
    private String secretKeyString;

    @Value("${jwt.access.token.expiration.ms:3600000}") // 1 hour
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh.token.expiration.ms:604800000}") // 7 days
    private long refreshTokenExpirationMs;

    private Key secretKey;

    @PostConstruct
    public void init() {
        // Ensure the secret key is strong enough for HS256, or use a more appropriate algorithm if needed
        if (secretKeyString.length() < 32) {
            // This is a simplified check. For production, ensure proper key management.
            // System.err.println("Warning: JWT secret key is too short. Using a default secure key for now. Please configure a strong jwt.secret in application properties.");
            // For HS256, the key should be at least 256 bits (32 bytes)
            this.secretKeyString = "defaultSecretKeyWhichIsVeryLongAndSecureAndAtLeast256Bits";
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateAccessToken(String username, Long userId, Role role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role.name()); // Store role name
        return createToken(claims, username, accessTokenExpirationMs);
    }

    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
         claims.put("userId", userId); // Include userId for refresh token validation against user
        return createToken(claims, username, refreshTokenExpirationMs);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) // Typically email or username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) { // Covers various JWT exceptions
            return false;
        }
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    public Role extractRole(String token) {
        Claims claims = extractAllClaims(token);
        String roleName = claims.get("role", String.class);
        return Role.valueOf(roleName);
    }
}
