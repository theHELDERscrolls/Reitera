package com.helderruiz.reitera_backend.modules.study.dto;

import java.util.Map;

/**
 * Projection returned by GET /study/due for cards the user should study now.
 * state reflects the FSRS learning state (1=learning, 2=review, 3=relearning),
 * or 0 when the card has never been studied by this user (brand-new card).
 */
public record DueCardDTO(
        Integer id,
        Integer deckId,
        String type,
        String question,
        Map<String, Object> answerJson,
        String explanation,
        Integer state
) {
}
