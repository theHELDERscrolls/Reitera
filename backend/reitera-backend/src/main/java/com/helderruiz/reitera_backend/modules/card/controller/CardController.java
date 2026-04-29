package com.helderruiz.reitera_backend.modules.card.controller;

import com.helderruiz.reitera_backend.modules.card.dto.CardRequestDTO;
import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.service.CardService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing CRUD endpoints for Card management.
 * Cards are nested under their parent Deck in all routes.
 * All endpoints require a valid JWT token (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/decks/{deckId}/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    /**
     * Creates a new card inside the specified deck.
     * POST /api/v1/decks/{deckId}/cards
     */
    @PostMapping
    public ResponseEntity<CardResponseDTO> createCard(
            @PathVariable Integer deckId,
            @Valid @RequestBody CardRequestDTO dto,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(deckId, dto, user));
    }

    /**
     * Returns a paginated list of cards belonging to the specified deck.
     * GET /api/v1/decks/{deckId}/cards?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<CardResponseDTO>> getCardsByDeck(
            @PathVariable Integer deckId,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(cardService.getCardsByDeck(deckId, user, pageable));
    }

    /**
     * Returns a single card by ID within the specified deck.
     * GET /api/v1/decks/{deckId}/cards/{cardId}
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> getCardById(
            @PathVariable Integer deckId,
            @PathVariable Integer cardId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(cardService.getCardById(deckId, cardId, user));
    }

    /**
     * Updates an existing card. Only the deck owner can perform this operation.
     * PUT /api/v1/decks/{deckId}/cards/{cardId}
     */
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> updateCard(
            @PathVariable Integer deckId,
            @PathVariable Integer cardId,
            @Valid @RequestBody CardRequestDTO dto,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(cardService.updateCard(deckId, cardId, dto, user));
    }

    /**
     * Deletes a card by ID. Only the deck owner can perform this operation.
     * DELETE /api/v1/decks/{deckId}/cards/{cardId}
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Integer deckId,
            @PathVariable Integer cardId,
            @AuthenticationPrincipal User user) {

        cardService.deleteCard(deckId, cardId, user);
        return ResponseEntity.noContent().build(); // HTTP 204: success with no response body
    }
}
