package com.helderruiz.reitera_backend.modules.card.dto;

import java.util.Map;
import java.util.Set;

/**
 * DTO representing the output data for a Card.
 * Flattens the Tag relationship into a lightweight nested summary for easier frontend consumption.
 */
public record CardResponseDTO(
        Integer id,
        Integer deckId,
        String type,
        String question,
        Map<String, Object> answerJson,
        String explanation,
        Set<TagSummary> tags,
        Integer state
) {

    /**
     * Lightweight tag representation embedded in the card response.
     * Avoids creating a separate file for a simple projection.
     */
    public record TagSummary(Integer id, String name, String hexColor) {
    }
}
