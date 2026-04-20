package com.helderruiz.reitera_backend.modules.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String roleName,
        LocalDateTime createdAt
) {}