package com.helderruiz.reitera_backend.modules.deck.dto;

public record DeckStatsDTO(
        long totalCards,
        long newCards,
        long learningCards,
        long reviewCards,
        long relearningCards,
        long dueCards
) {
}
