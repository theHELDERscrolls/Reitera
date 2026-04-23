package com.helderruiz.reitera_backend.modules.dashboard.dto;

/**
 * One entry in the "Last decks studied" dashboard panel.
 *
 * @param deckId         primary key of the deck
 * @param name           deck title
 * @param category       category name, or {@code null} if uncategorised
 * @param newCount       cards the user has never reviewed in this deck
 * @param dueCount       due learning/review cards (FSRS states 1 and 2, nextReview ≤ now)
 * @param relearningCount due relearning cards (FSRS state 3, nextReview ≤ now)
 * @param lastStudied    ISO-8601 timestamp of the most recent review in this deck
 */
public record LastStudiedDeckDTO(
        int deckId,
        String name,
        String category,
        long newCount,
        long dueCount,
        long relearningCount,
        String lastStudied
) {
}
