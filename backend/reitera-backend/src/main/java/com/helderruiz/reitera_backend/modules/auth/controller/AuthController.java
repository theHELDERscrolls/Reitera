package com.helderruiz.reitera_backend.modules.auth.controller;

import com.helderruiz.reitera_backend.modules.auth.model.RefreshToken;
import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.auth.service.RefreshTokenService;
import com.helderruiz.reitera_backend.modules.user.dto.AuthResponseDTO;
import com.helderruiz.reitera_backend.modules.user.dto.RefreshRequestDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserLoginDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserRegisterDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsible for handling public authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    /**
     * Handles user registration.
     * Validates input, creates a new user, and returns the created user data.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegisterDTO dto) {
        UserResponseDTO response = userService.registerUser(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Handles user authentication (login).
     * Verifies credentials and issues an access token and a refresh token upon success.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody UserLoginDTO dto) {
        AuthResponseDTO response = userService.loginUser(dto);

        return ResponseEntity.ok(response);
    }

    /**
     * Issues a new access token and a rotated refresh token given a valid refresh token.
     * The old refresh token is revoked immediately after use.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        RefreshToken current = refreshTokenService.validateRefreshToken(dto.refreshToken());

        String newAccessToken = jwtService.generateToken(
                current.getUser().getUsername(),
                current.getUser().getRole().getName()
        );
        String newRefreshToken = refreshTokenService.rotateRefreshToken(current);

        return ResponseEntity.ok(new AuthResponseDTO(newAccessToken, newRefreshToken, "Token refreshed"));
    }

    /**
     * Revokes all active refresh tokens for the user identified by the provided refresh token.
     * The access token may already be expired when this endpoint is called.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO dto) {
        RefreshToken token = refreshTokenService.validateRefreshToken(dto.refreshToken());
        refreshTokenService.revokeAllTokens(token.getUser());

        return ResponseEntity.noContent().build();
    }
}