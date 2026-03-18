package com.helderruiz.reitera_backend.modules.study.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Payload for submitting a completed study session.
 * Exactly one of deckId or categoryId must be provided — validated in StudyService.
 * Use deckId to study a single deck, categoryId to study all decks in a category.
 */
public record StudySessionRequestDTO(

        // Provide deckId for single-deck sessions
        Integer deckId,

        // Provide categoryId for cross-deck sessions (all decks in a category)
        Integer categoryId,

        // @Valid cascades validation rules into each CardRatingDTO inside the list
        @NotEmpty(message = "Ratings list cannot be empty")
        @Valid
        List<CardRatingDTO> ratings
) {
}
