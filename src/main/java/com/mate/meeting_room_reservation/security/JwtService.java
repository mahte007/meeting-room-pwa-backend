package com.mate.meeting_room_reservation.security;

import com.mate.meeting_room_reservation.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET =
            "this-is-a-demo-secret-key-for-meeting-room-reservation-app-please-change";

    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 2; // 2 hours

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AppUser user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", "ROLE_" + user.getRole().name())
                .claim("userId", user.getId())
                .claim("employeeId", user.getEmployee() != null ? user.getEmployee().getId() : null)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractEmployeeId(String token) {
        Object employeeId = extractAllClaims(token).get("employeeId");

        if (employeeId == null) {
            return null;
        }

        if (employeeId instanceof Integer id) {
            return id.longValue();
        }

        if (employeeId instanceof Long id) {
            return id;
        }

        return Long.valueOf(employeeId.toString());
    }

    public boolean isTokenValid(String token) {
        // The parser verifies the signature and expiration and throws if either check fails
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}