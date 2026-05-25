package com.helderruiz.reitera_backend.modules.card.controller;

import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.service.CardService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing a cross-deck card listing endpoint for the authenticated user.
 * Separated from CardController because it operates at /api/v1/cards rather than
 * the nested /api/v1/decks/{deckId}/cards route.
 * All endpoints require a valid JWT token (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CardListController {

    private final CardService cardService;

    /**
     * Returns a paginated list of all cards owned by the authenticated user, across all decks.
     * Supports optional filters: question (partial match), type (exact), state (FSRS or null).
     * GET /api/v1/cards?question=...&type=BASIC&state=1&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<CardResponseDTO>> getAllCards(
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer state,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "question") Pageable pageable) {

        return ResponseEntity.ok(cardService.getAllCards(user, question, type, state, pageable));
    }
}
