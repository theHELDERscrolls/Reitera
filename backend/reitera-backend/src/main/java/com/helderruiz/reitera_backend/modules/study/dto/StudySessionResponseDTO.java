package com.helderruiz.reitera_backend.modules.study.dto;

/**
 * Response returned after a study session is successfully processed.
 * One of deckId or categoryId will be populated depending on the session type.
 * Confirms how many cards were reviewed and scheduled by the FSRS algorithm.
 */
public record StudySessionResponseDTO(

        // Populated for single-deck sessions; null for category sessions
        Integer deckId,

        // Populated for category sessions; null for single-deck sessions
        Integer categoryId,

        // Total number of card ratings processed in this session
        Integer cardsReviewed
) {
}
