package com.helderruiz.reitera_backend.modules.deck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Represents a tag the client wants to create on the fly while saving a card.
 * If a tag with the same name already exists for this user, it is reused (find-or-create).
 */
public record NewTagDTO(
        @NotBlank(message = "Tag name is mandatory")
        @Size(max = 30, message = "Tag name cannot exceed 30 characters")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code like #1A2B3C")
        String hexColor
) {
}
