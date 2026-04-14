package com.helderruiz.reitera_backend.modules.card.dto;

import com.helderruiz.reitera_backend.modules.deck.dto.NewTagDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DTO for creating or updating a Card.
 * Tags can be attached in two ways:
 * - tagIds: IDs of tags the user selected from their existing tag list.
 * - newTags: name+color pairs for tags the user typed that don't exist yet (find-or-create).
 */
public record CardRequestDTO(

        @NotBlank(message = "Type is mandatory")
        @Pattern(regexp = "BASIC|CLOZE|MULTIPLE_CHOICE|TRUE_FALSE", message = "Type must be BASIC, CLOZE, MULTIPLE_CHOICE, or TRUE_FALSE")
        String type,

        @NotBlank(message = "Question is mandatory")
        String question,

        @NotNull(message = "Answer JSON is mandatory")
        Map<String, Object> answerJson,

        String explanation,

        Set<Integer> tagIds,

        List<NewTagDTO> newTags
) {
}
