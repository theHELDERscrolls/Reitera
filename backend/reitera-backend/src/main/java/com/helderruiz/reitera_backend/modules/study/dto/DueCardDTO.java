package com.helderruiz.reitera_backend.modules.study.dto;

import java.util.Map;
import java.util.Set;

/**
 * Response DTO representing a card that is due for review.
 * Combines the card's content with its current FSRS state so the frontend
 * can display the question/answer and show the appropriate study badge
 * (New, Learning, Review, Relearning).
 */
public record DueCardDTO(

        Integer id,
        Integer deckId,
        String type,
        String question,

        // JSONB answer — structure varies by card type (BASIC, MULTIPLE_CHOICE, etc.)
        Map<String, Object> answerJson,

        String explanation,
        Set<TagSummary> tags,

        // FSRS state: 0=New, 1=Learning, 2=Review, 3=Relearning
        Integer state
) {

    /**
     * Lightweight tag projection — same shape as CardResponseDTO.TagSummary
     * but kept here to avoid a cross-module DTO dependency.
     */
    public record TagSummary(Integer id, String name, String hexColor) {
    }
}
