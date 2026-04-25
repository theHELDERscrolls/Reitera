package com.helderruiz.reitera_backend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequestDTO(
        @NotBlank(message = "Refresh token must not be blank")
        @Size(max = 512, message = "Invalid refresh token")
        String refreshToken
) {
}
