package com.helderruiz.reitera_backend.modules.study.controller;

import com.helderruiz.reitera_backend.modules.study.dto.DueCardDTO;
import com.helderruiz.reitera_backend.modules.study.dto.StudySessionRequestDTO;
import com.helderruiz.reitera_backend.modules.study.dto.StudySessionResponseDTO;
import com.helderruiz.reitera_backend.modules.study.service.StudyService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing the two core study endpoints.
 * All endpoints require a valid JWT token (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /**
     * Returns all cards due for review for the authenticated user.
     * Provide exactly one of: deckId (single deck) or categoryId (all decks in a category).
     * <p>
     * GET /api/v1/study/due?deckId={id}
     * GET /api/v1/study/due?categoryId={id}
     */
    @GetMapping("/due")
    public ResponseEntity<List<DueCardDTO>> getDueCards(
            @RequestParam(required = false) Integer deckId,
            @RequestParam(required = false) Integer categoryId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(studyService.getDueCards(deckId, categoryId, user));
    }

    /**
     * Processes a completed study session: runs FSRS for each rated card,
     * persists the updated StudyProgress, and writes a ReviewLog entry.
     * The entire batch is processed in a single transaction.
     * Accepts either deckId or categoryId in the request body.
     * <p>
     * POST /api/v1/study/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<StudySessionResponseDTO> processSession(
            @Valid @RequestBody StudySessionRequestDTO dto,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(studyService.processSession(dto, user));
    }
}
