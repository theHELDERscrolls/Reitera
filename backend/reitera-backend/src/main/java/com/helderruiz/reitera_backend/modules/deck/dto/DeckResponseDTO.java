package com.helderruiz.reitera_backend.modules.deck.dto;

import java.time.LocalDateTime;

/**
 * Projection returned by deck list and detail endpoints.
 * newCount, dueCount, and relearningCount are FSRS-based per-user study counters
 * computed at query time; they are 0 in single-deck GET /decks/{id} responses.
 */
public record DeckResponseDTO(
        Integer id,
        String title,
        String description,
        String authorName,
        Integer categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long newCount,
        long dueCount,
        long relearningCount
) {
}