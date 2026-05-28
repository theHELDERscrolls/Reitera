package com.helderruiz.reitera_backend.modules.note.controller;

import com.helderruiz.reitera_backend.modules.note.dto.NoteRequestDTO;
import com.helderruiz.reitera_backend.modules.note.dto.NoteResponseDTO;
import com.helderruiz.reitera_backend.modules.note.service.NoteService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/decks/{deckId}/notes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteResponseDTO> createNote(
            @PathVariable Integer deckId,
            @Valid @RequestBody NoteRequestDTO dto,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.createNote(deckId, dto, user));
    }

    @GetMapping
    public ResponseEntity<Page<NoteResponseDTO>> getNotesByDeck(
            @PathVariable Integer deckId,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(noteService.getNotesByDeck(deckId, user, pageable));
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<NoteResponseDTO> getNoteById(
            @PathVariable Integer deckId,
            @PathVariable Integer noteId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(noteService.getNoteById(deckId, noteId, user));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<NoteResponseDTO> updateNote(
            @PathVariable Integer deckId,
            @PathVariable Integer noteId,
            @Valid @RequestBody NoteRequestDTO dto,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(noteService.updateNote(deckId, noteId, dto, user));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Integer deckId,
            @PathVariable Integer noteId,
            @AuthenticationPrincipal User user) {

        noteService.deleteNote(deckId, noteId, user);
        return ResponseEntity.noContent().build();
    }
}
