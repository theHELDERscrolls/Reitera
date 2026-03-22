package com.helderruiz.reitera_backend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken
) {
}
