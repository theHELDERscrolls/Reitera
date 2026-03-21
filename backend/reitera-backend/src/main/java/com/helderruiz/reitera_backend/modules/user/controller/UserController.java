package com.helderruiz.reitera_backend.modules.user.controller;

import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user profile endpoints.
 * All endpoints require a valid JWT token (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns the profile of the authenticated user.
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getMe(user));
    }
}
