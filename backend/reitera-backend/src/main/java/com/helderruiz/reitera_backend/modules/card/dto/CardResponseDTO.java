package com.helderruiz.reitera_backend.modules.card.dto;

import java.util.Map;

/**
 * Projection returned by card CRUD endpoints.
 * state reflects the FSRS learning state (1=learning, 2=review, 3=relearning),
 * or null when the authenticated user has no StudyProgress for this card yet.
 */
public record CardResponseDTO(
        Integer id,
        Integer deckId,
        String type,
        String question,
        Map<String, Object> answerJson,
        String explanation,
        Integer state
) {
}
