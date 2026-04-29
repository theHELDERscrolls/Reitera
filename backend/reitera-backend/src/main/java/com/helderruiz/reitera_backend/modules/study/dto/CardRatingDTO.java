package com.helderruiz.reitera_backend.modules.study.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a single card rating submitted by the user during a study session.
 * Rating scale: 1 (Again), 2 (Hard), 3 (Good), 4 (Easy).
 */
public record CardRatingDTO(

        @NotNull(message = "Card ID is mandatory")
        Integer cardId,

        // FSRS accepts ratings from 1 to 4 only
        @NotNull(message = "Rating is mandatory")
        @Min(value = 1, message = "Rating must be at least 1 (Again)")
        @Max(value = 4, message = "Rating must be at most 4 (Easy)")
        Integer rating
) {
}
