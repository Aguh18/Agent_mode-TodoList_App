package com.example.demo.utils;

import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtils {

    private final String jwtSecret = "secretKeyRahasiaBangetYangPanjang12345"; // minimal 32 karakter
    private final long jwtExpirationMs = 86400000; // 24 jam

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // === Generate Token ===
    public String generateToken(String username, Long userId, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // === Ambil token dari Authorization Header ===
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // === Extract Claims ===
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // === Getters ===
    public String getUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public Long getUserId(String token) {
        Object userIdObj = getAllClaimsFromToken(token).get("userId");
        return (userIdObj instanceof Number) ? ((Number) userIdObj).longValue() : null;
    }

    public String getRole(String token) {
        return (String) getAllClaimsFromToken(token).get("role");
    }

    // === Helper methods untuk extract dari HttpServletRequest ===
    public String getUsernameFromRequest(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && validateToken(token)) {
            return getUsername(token);
        }
        return null;
    }

    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && validateToken(token)) {
            return getUserId(token);
        }
        return null;
    }

    public String getRoleFromRequest(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && validateToken(token)) {
            return getRole(token);
        }
        return null;
    }

    // === Validate ===
    public boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
