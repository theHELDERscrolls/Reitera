package com.helderruiz.reitera_backend.modules.auth.service;

import com.helderruiz.reitera_backend.core.exception.InvalidRefreshTokenException;
import com.helderruiz.reitera_backend.modules.auth.model.RefreshToken;
import com.helderruiz.reitera_backend.modules.auth.repository.RefreshTokenRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    private User mockUser;
    private RefreshToken validToken;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "expirationDays", 7L);
        mockUser = User.builder().id(UUID.randomUUID()).build();
        validToken = RefreshToken.builder()
                .user(mockUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void createRefreshToken_savesHashedToken_returnsRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        String rawToken = refreshTokenService.createRefreshToken(mockUser);

        verify(refreshTokenRepository).save(captor.capture());
        assertThat(rawToken).isNotBlank();
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
    }

    @Test
    void validateRefreshToken_returnsToken_whenValid() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(validToken));

        RefreshToken result = refreshTokenService.validateRefreshToken(UUID.randomUUID().toString());

        assertThat(result).isEqualTo(validToken);
    }

    @Test
    void validateRefreshToken_throws_whenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(UUID.randomUUID().toString()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateRefreshToken_throws_whenRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(UUID.randomUUID().toString()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateRefreshToken_throws_whenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(UUID.randomUUID().toString()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateRefreshToken_revokesOld_createsNew() {
        String newToken = refreshTokenService.rotateRefreshToken(validToken);

        assertThat(validToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        assertThat(newToken).isNotBlank();
    }

    @Test
    void revokeAllTokens_delegatesToRepository() {
        refreshTokenService.revokeAllTokens(mockUser);

        verify(refreshTokenRepository).revokeAllByUser(mockUser);
    }
}
