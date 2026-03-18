package com.helderruiz.reitera_backend.modules.deck.controller;

import com.helderruiz.reitera_backend.modules.deck.dto.DeckRequestDTO;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.service.DeckService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing CRUD endpoints for Deck management.
 * All endpoints require a valid JWT token (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    /**
     * Creates a new deck for the authenticated user.
     * POST /api/v1/decks
     */
    @PostMapping
    public ResponseEntity<DeckResponseDTO> createDeck(
            @Valid @RequestBody DeckRequestDTO dto,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.createDeck(dto, user));
    }

    /**
     * Returns all decks owned by the authenticated user.
     * GET /api/v1/decks
     */
    @GetMapping
    public ResponseEntity<List<DeckResponseDTO>> getUserDecks(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deckService.getUserDecks(user));
    }

    /**
     * Returns a single deck by ID. Only the owner can access it.
     * GET /api/v1/decks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeckResponseDTO> getDeckById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(deckService.getDeckById(id, user));
    }

    /**
     * Updates an existing deck. Only the owner can perform this operation.
     * PUT /api/v1/decks/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeckResponseDTO> updateDeck(
            @PathVariable Integer id,
            @Valid @RequestBody DeckRequestDTO dto,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(deckService.updateDeck(id, dto, user));
    }

    /**
     * Deletes a deck by ID. Only the owner can perform this operation.
     * DELETE /api/v1/decks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user) {

        deckService.deleteDeck(id, user);
        return ResponseEntity.noContent().build(); // HTTP 204: success with no response body
    }
}
