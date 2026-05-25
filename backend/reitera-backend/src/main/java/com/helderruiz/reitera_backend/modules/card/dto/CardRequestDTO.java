package com.helderruiz.reitera_backend.modules.card.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public record CardRequestDTO(

        @NotBlank(message = "Type is mandatory")
        @Pattern(regexp = "BASIC|MULTIPLE_CHOICE|TRUE_FALSE", message = "Type must be BASIC, MULTIPLE_CHOICE, or TRUE_FALSE")
        String type,

        @NotBlank(message = "Question is mandatory")
        @Size(max = 5000, message = "Question cannot exceed 5000 characters")
        String question,

        @NotNull(message = "Answer JSON is mandatory")
        Map<String, Object> answerJson,

        @Size(max = 2000, message = "Explanation cannot exceed 2000 characters")
        String explanation
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_ANSWER_JSON_BYTES = 10_000;

    /**
     * Caps the serialized JSON payload at 10 KB to prevent oversized submissions
     * from bloating the JSONB column or stressing parsers.
     */
    @AssertTrue(message = "Answer JSON cannot exceed 10000 bytes when serialized")
    public boolean isAnswerJsonWithinLimit() {
        if (answerJson == null) {
            return true;
        }
        try {
            byte[] bytes = MAPPER.writeValueAsString(answerJson).getBytes(StandardCharsets.UTF_8);
            return bytes.length <= MAX_ANSWER_JSON_BYTES;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
