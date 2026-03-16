package com.helderruiz.reitera_backend.modules.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${api.security.jwt.secret-key}")
    private String secretKey;

    @Value("${api.security.jwt.expiration-time}")
    private long jwtExpiration;

    /**
     * Generates a JWT token using HMAC-SHA algorithm.
     * Injects custom claims (e.g., role), sets the subject, timestamps, and cryptographically signs it.
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .claims(Map.of("role", role)) // Custom payload data
                .subject(username)                // The principal (typically email/username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())         // Applies the cryptographic signature
                .compact();                       // Serializes into the standard Base64Url-encoded JWT string
    }

    /**
     * Extracts the subject (username/email) from the token's payload.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates the token by checking if the subject matches the UserDetails
     * and ensuring the token has not passed its expiration date.
     */
    public boolean isTokenValid(String token, String userEmail) {
        final String username = extractUsername(token);
        return (username.equals(userEmail)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract a specific claim using a functional interface.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT. Crucially, it verifies the cryptographic signature against the server's secret key.
     * If the token was tampered with, this method will throw a SignatureException.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey()) // Cryptographic validation step
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generates a cryptographic SecretKey instance from the plaintext configuration string
     * using UTF-8 encoding. Required by the JJWT library for HMAC-SHA operations.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}