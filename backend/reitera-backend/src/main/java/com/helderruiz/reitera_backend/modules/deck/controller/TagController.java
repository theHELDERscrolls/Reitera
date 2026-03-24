package com.helderruiz.reitera_backend.modules.deck.controller;

import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.service.CardService;
import com.helderruiz.reitera_backend.modules.deck.dto.TagResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.service.TagService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@SecurityRequirement(name = "bearerAuth")
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
     * Returns a paginated list of cards owned by the authenticated user that have the specified tag.
     * GET /api/v1/tags/{tagId}/cards?page=0&size=20
     */
    @GetMapping("/{tagId}/cards")
    public ResponseEntity<Page<CardResponseDTO>> getCardsByTag(
            @PathVariable Integer tagId,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(cardService.getCardsByTag(tagId, user, pageable));
    }
}
