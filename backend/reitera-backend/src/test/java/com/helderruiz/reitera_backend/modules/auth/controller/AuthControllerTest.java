package com.helderruiz.reitera_backend.modules.auth.controller;

import com.helderruiz.reitera_backend.core.email.PasswordResetService;
import com.helderruiz.reitera_backend.core.exception.InvalidRefreshTokenException;
import com.helderruiz.reitera_backend.core.exception.TokenExpiredException;
import com.helderruiz.reitera_backend.modules.auth.model.RefreshToken;
import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.auth.service.RefreshTokenService;
import com.helderruiz.reitera_backend.modules.user.dto.AuthResponseDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.service.UserService;
import com.helderruiz.reitera_backend.config.SecurityConfig;
import com.helderruiz.reitera_backend.core.email.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {
    private User mockUser;
    private RefreshToken mockRefreshToken;
    private UserResponseDTO userResponse;
    private AuthResponseDTO authResponse;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("reitera@test.com")
                .role(studentRole)
                .build();
        mockRefreshToken = RefreshToken.builder()
                .user(mockUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        userResponse = new UserResponseDTO(
                mockUser.getId(), "reitera", "reitera@test.com",
                "Reitera", "Testez", "STUDENT", LocalDateTime.now());
        authResponse = new AuthResponseDTO("access-token", "refresh-token", "Login successful");
    }

    @Test
    void register_returns201_whenValid() throws Exception {
        when(userService.registerUser(any())).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reitera\",\"email\":\"reitera@test.com\","
                                + "\"password\":\"Test@1234\",\"confirmPassword\":\"Test@1234\","
                                + "\"firstName\":\"Reitera\",\"lastName\":\"Testez\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("reitera@test.com"))
                .andExpect(jsonPath("$.roleName").value("STUDENT"));
    }

    @Test
    void register_returns400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200_withTokens_whenCredentialsValid() throws Exception {
        when(userService.loginUser(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reitera@test.com\",\"password\":\"Test@1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_returns400_whenCredentialsFail() throws Exception {
        when(userService.loginUser(any())).thenThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reitera@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_returns200_withNewTokens_whenValid() throws Exception {
        when(refreshTokenService.validateRefreshToken(any())).thenReturn(mockRefreshToken);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("new-access-token");
        when(refreshTokenService.rotateRefreshToken(any())).thenReturn("new-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_returns401_whenRefreshTokenInvalid() throws Exception {
        when(refreshTokenService.validateRefreshToken(any()))
                .thenThrow(new InvalidRefreshTokenException("Token expired"));

        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_returns204() throws Exception {
        when(refreshTokenService.validateRefreshToken(any())).thenReturn(mockRefreshToken);
        doNothing().when(refreshTokenService).revokeAllTokens(any());

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-valid-token\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void verifyEmail_returns200_whenTokenValid() throws Exception {
        doNothing().when(emailVerificationService).verifyToken(anyString());

        mockMvc.perform(get("/api/v1/auth/verify").param("token", "valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void verifyEmail_returns400_whenTokenInvalid() throws Exception {
        doThrow(new IllegalArgumentException("Invalid token"))
                .when(emailVerificationService).verifyToken(anyString());

        mockMvc.perform(get("/api/v1/auth/verify").param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_returns400_whenTokenExpired() throws Exception {
        doThrow(new IllegalArgumentException("Expired token"))
                .when(emailVerificationService).verifyToken(anyString());

        mockMvc.perform(get("/api/v1/auth/verify").param("token", "expired-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerification_returns200_always() throws Exception {
        doNothing().when(emailVerificationService).resendToken(anyString());

        mockMvc.perform(post("/api/v1/auth/resend-verification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reitera@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void resendVerification_returns400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/resend-verification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_returns200_whenEmailKnown() throws Exception {
        doNothing().when(passwordResetService).sendResetToken(anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reitera@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_returns200_whenEmailUnknown() throws Exception {
        doNothing().when(passwordResetService).sendResetToken(anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_returns200_whenTokenValid() throws Exception {
        doNothing().when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-token\",\"newPassword\":\"NewPass@1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_returns400_whenTokenInvalid() throws Exception {
        doThrow(new IllegalArgumentException("Invalid or already used reset token"))
                .when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad-token\",\"newPassword\":\"NewPass@1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_returns410_whenTokenExpired() throws Exception {
        doThrow(new TokenExpiredException("Password reset link has expired"))
                .when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"expired-token\",\"newPassword\":\"NewPass@1234\"}"))
                .andExpect(status().isGone());
    }
}
