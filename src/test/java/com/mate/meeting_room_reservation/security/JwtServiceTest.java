package com.mate.meeting_room_reservation.security;

import com.mate.meeting_room_reservation.entity.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.mate.meeting_room_reservation.support.TestData.admin;
import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.employeeUser;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @Test
    void generatedTokenIsValidAndCarriesUsername() {
        String token = jwtService.generateToken(admin());

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void employeeTokenCarriesEmployeeId() {
        String token = jwtService.generateToken(employeeUser(employee(7L)));

        assertThat(jwtService.extractEmployeeId(token)).isEqualTo(7L);
    }

    @Test
    void adminTokenHasNoEmployeeId() {
        String token = jwtService.generateToken(admin());

        assertThat(jwtService.extractEmployeeId(token)).isNull();
    }

    @Test
    void tamperedTokenIsInvalid() {
        String token = jwtService.generateToken(admin());
        String[] parts = token.split("\\.");

        AppUser other = employeeUser(employee(2L));
        String otherPayload = jwtService.generateToken(other).split("\\.")[1];
        String tampered = parts[0] + "." + otherPayload + "." + parts[2];

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKeyIsInvalid() {
        SecretKey foreignKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-that-is-long-enough".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject("admin").signWith(foreignKey).compact();

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        SecretKey key = ReflectionTestUtils.invokeMethod(jwtService, "getSigningKey");
        String token = Jwts.builder()
                .subject("admin")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void malformedOrEmptyTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }
}
