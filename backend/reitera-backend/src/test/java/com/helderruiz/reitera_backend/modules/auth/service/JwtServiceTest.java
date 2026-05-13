package com.helderruiz.reitera_backend.modules.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private JwtService jwtService;
    private String validToken;

    private static final String TEST_SECRET = "test-secret-key-for-hmac-sha-that-is-long-enough-to-be-valid";
    private static final String EMAIL = "user@test.com";
    private static final String ROLE = "STUDENT";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
        validToken = jwtService.generateToken(EMAIL, ROLE);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        assertThat(jwtService.generateToken(EMAIL, ROLE)).isNotNull().isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectEmail() {
        assertThat(jwtService.extractUsername(validToken)).isEqualTo(EMAIL);
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        assertThat(jwtService.isTokenValid(validToken, EMAIL)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForWrongEmail() {
        assertThat(jwtService.isTokenValid(validToken, "other@test.com")).isFalse();
    }
}
