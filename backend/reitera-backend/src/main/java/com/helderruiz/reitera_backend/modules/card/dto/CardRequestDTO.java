package com.helderruiz.reitera_backend.modules.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;
import java.util.Set;

/**
 * DTO for creating or updating a Card.
 * Includes validation rules to ensure data integrity before reaching the Service layer.
 */
public record CardRequestDTO(

        @NotBlank(message = "Type is mandatory")
        @Pattern(regexp = "BASIC|CLOZE|MULTIPLE_CHOICE", message = "Type must be BASIC, CLOZE, or MULTIPLE_CHOICE")
        String type,

        @NotBlank(message = "Question is mandatory")
        String question,

        // Dynamic JSON object: structure varies by card type (e.g., {"answer": "..."} or {"options": [...]})
        @NotNull(message = "Answer JSON is mandatory")
        Map<String, Object> answerJson,

        String explanation,

        // Optional: IDs of existing tags to attach to this card
        Set<Integer> tagIds
) {
}
