package com.helderruiz.reitera_backend.modules.deck.dto;

import java.time.LocalDateTime;

/**
 * DTO representing the output data for a Deck.
 * Flattens relationships (like Category and User) into simple strings for easier frontend consumption.
 */
public record DeckResponseDTO(
        Integer id,
        String title,
        String description,
        boolean isPublic,
        String authorName,
        Integer categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}