package com.helderruiz.reitera_backend.modules.deck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a Deck.
 * Includes validation rules to ensure data integrity before reaching the Service layer.
 */
public record DeckRequestDTO(
        @NotBlank(message = "Title is mandatory")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        boolean isPublic,

        Integer categoryId,

        @Size(max = 50, message = "Category name cannot exceed 50 characters")
        String categoryName
) {
}
