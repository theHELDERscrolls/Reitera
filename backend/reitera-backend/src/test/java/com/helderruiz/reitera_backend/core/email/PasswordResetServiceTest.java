package com.helderruiz.reitera_backend.core.email;

import com.helderruiz.reitera_backend.core.exception.TokenExpiredException;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private User mockUser;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("reitera@test.com")
                .firstName("Reitera")
                .lastName("Testez")
                .role(studentRole)
                .emailVerified(true)
                .build();
    }

    @Test
    void sendResetToken_silentlyIgnores_whenEmailUnknown() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        passwordResetService.sendResetToken("unknown@test.com");

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void sendResetToken_savesHashedTokenAndSendsEmail_whenEmailKnown() {
        when(userRepository.findByEmail("reitera@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        passwordResetService.sendResetToken("reitera@test.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPasswordResetToken()).isNotNull();
        assertThat(saved.getPasswordResetTokenExpiresAt()).isAfter(LocalDateTime.now());
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resetPassword_throwsIllegalArgument_whenTokenNotFound() {
        when(userRepository.findByPasswordResetToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", "NewPass@1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or already used");
    }

    @Test
    void resetPassword_throwsTokenExpired_whenTokenExpired() {
        mockUser.setPasswordResetToken("hashed-token");
        mockUser.setPasswordResetTokenExpiresAt(LocalDateTime.now().minusHours(2));
        when(userRepository.findByPasswordResetToken(anyString())).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> passwordResetService.resetPassword("raw-token", "NewPass@1"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void resetPassword_encodesPasswordAndNullifiesToken_whenValid() {
        mockUser.setPasswordResetToken("hashed-token");
        mockUser.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        when(userRepository.findByPasswordResetToken(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("NewPass@1")).thenReturn("encoded-password");
        when(userRepository.save(any())).thenReturn(mockUser);

        passwordResetService.resetPassword("raw-token", "NewPass@1");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getPasswordResetToken()).isNull();
        assertThat(saved.getPasswordResetTokenExpiresAt()).isNull();
    }
}
