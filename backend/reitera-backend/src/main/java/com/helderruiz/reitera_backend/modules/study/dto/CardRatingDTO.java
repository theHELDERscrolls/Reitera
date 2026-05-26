package com.helderruiz.reitera_backend.modules.study.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a single card rating submitted by the user during a study session.
 * Rating scale: 1 (Forgotten), 3 (Remembered).
 */
public record CardRatingDTO(

        @NotNull(message = "Card ID is mandatory")
        Integer cardId,

        // Accepted ratings: 1 (Forgotten) and 3 (Remembered) only
        @NotNull(message = "Rating is mandatory")
        @Min(value = 1, message = "Rating must be 1 (Forgotten) or 3 (Remembered)")
        @Max(value = 3, message = "Rating must be 1 (Forgotten) or 3 (Remembered)")
        Integer rating
) {
}
