package com.helderruiz.reitera_backend.modules.note.dto;

import java.time.OffsetDateTime;

public record NoteResponseDTO(
        Integer id,
        Integer deckId,
        String type,
        String content,
        String explanation,
        int cardCount,
        OffsetDateTime createdAt
) {
}
