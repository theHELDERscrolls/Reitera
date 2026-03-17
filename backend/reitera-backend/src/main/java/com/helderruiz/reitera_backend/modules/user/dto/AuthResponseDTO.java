package com.helderruiz.reitera_backend.modules.user.dto;

public record AuthResponseDTO(
        String token,
        String message
) {
}