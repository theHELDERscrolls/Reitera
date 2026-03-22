package com.helderruiz.reitera_backend.modules.auth.service;

import com.helderruiz.reitera_backend.core.exception.InvalidRefreshTokenException;
import com.helderruiz.reitera_backend.modules.auth.model.RefreshToken;
import com.helderruiz.reitera_backend.modules.auth.repository.RefreshTokenRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Manages the lifecycle of refresh tokens: creation, validation, rotation, and revocation.
 * Raw token values are never stored — only their SHA-256 hash is persisted.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${api.security.refresh-token.expiration-days}")
    private long expirationDays;

    /**
     * Generates a new refresh token for the given user and persists its hash.
     * Returns the raw token value to be sent to the client exactly once.
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Validates the raw token received from the client.
     * Hashes it, looks it up in the database, and checks it is not revoked or expired.
     * Returns the RefreshToken entity if valid; throws otherwise.
     */
    public RefreshToken validateRefreshToken(String rawToken) {
        String tokenHash = hash(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        return refreshToken;
    }

    /**
     * Rotates a refresh token: revokes the current one and issues a new one for the same user.
     * Limits the damage if a token is stolen — old tokens become useless after first use.
     */
    @Transactional
    public String rotateRefreshToken(RefreshToken current) {
        current.setRevoked(true);
        refreshTokenRepository.save(current);

        return createRefreshToken(current.getUser());
    }

    /**
     * Revokes all active refresh tokens for the given user.
     * Called on logout to invalidate all sessions across devices.
     */
    @Transactional
    public void revokeAllTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * Produces a SHA-256 hex digest of the given input string.
     */
    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
