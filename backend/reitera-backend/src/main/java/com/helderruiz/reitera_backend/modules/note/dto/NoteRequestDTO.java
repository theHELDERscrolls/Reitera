package com.helderruiz.reitera_backend.modules.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequestDTO(
        @NotBlank(message = "Note content must not be blank")
        @Size(max = 10000, message = "Note content must not exceed 10000 characters")
        String content,

        @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
        String explanation
) {
}
