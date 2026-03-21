package com.helderruiz.reitera_backend.modules.deck.controller;

import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.service.CardService;
import com.helderruiz.reitera_backend.modules.deck.dto.TagResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.service.TagService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing tag-related endpoints.
 * All endpoints require a valid JWT token (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final CardService cardService;

    /**
     * Returns the distinct tags used on cards belonging to the authenticated user's decks.
     * GET /api/v1/tags
     */
    @GetMapping
    public ResponseEntity<List<TagResponseDTO>> getMyTags(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tagService.getMyTags(user));
    }

    /**
     * Returns all cards owned by the authenticated user that have the specified tag.
     * GET /api/v1/tags/{tagId}/cards
     */
    @GetMapping("/{tagId}/cards")
    public ResponseEntity<List<CardResponseDTO>> getCardsByTag(
            @PathVariable Integer tagId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(cardService.getCardsByTag(tagId, user));
    }
}
