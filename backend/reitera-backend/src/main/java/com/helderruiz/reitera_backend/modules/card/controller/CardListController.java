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
 * REST controller for cross-deck card queries.
 * <p>
 * Unlike CardController (which is scoped to /decks/{deckId}/cards), this controller
 * exposes endpoints that operate across ALL decks owned by the authenticated user.
 * <p>
 * Currently exposes:
 *   GET /api/v1/cards — paginated list of all the user's cards with optional filters
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CardListController {

    private final CardService cardService;

    /**
     * Returns a paginated list of all cards owned by the current user, across all their decks.
     * <p>
     * All parameters are optional — omitting them returns all cards without filtering.
     *
     * @param question partial, case-insensitive match on the question text
     * @param type exact card type: BASIC, MULTIPLE_CHOICE, or TRUE_FALSE
     * @param state FSRS study state: -1 = never studied, 0 = New, 1 = Learning, 2 = Review, 3 = Relearning
     * @param tagId ID of a tag the card must have assigned
     * @param pageable pagination and sorting (default: page 0, size 20, sort by question asc)
     */
    @GetMapping
    public ResponseEntity<Page<CardResponseDTO>> getAllCards(
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) Integer tagId,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "question") Pageable pageable) {

        return ResponseEntity.ok(cardService.getAllCards(user, question, type, state, tagId, pageable));
    }
}
