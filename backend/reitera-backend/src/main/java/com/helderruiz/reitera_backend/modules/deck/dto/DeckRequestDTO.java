package com.helderruiz.reitera_backend.modules.deck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a deck.
 * Category resolution uses a two-step strategy:
 * - If categoryId is provided, the existing category is looked up by ID.
 * - Otherwise, if categoryName is provided, it is matched case-insensitively
 *   or created as a new Category if no match is found.
 * categoryId takes precedence over categoryName when both are supplied.
 */
public record DeckRequestDTO(
        @NotBlank(message = "Title is mandatory")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        Integer categoryId,

        @Size(max = 50, message = "Category name cannot exceed 50 characters")
        String categoryName
) {
}
