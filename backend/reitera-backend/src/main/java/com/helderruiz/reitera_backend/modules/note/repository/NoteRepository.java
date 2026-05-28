package com.helderruiz.reitera_backend.modules.note.repository;

import com.helderruiz.reitera_backend.modules.note.model.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Integer> {

    @EntityGraph(attributePaths = "cards")
    Page<Note> findAllByDeckId(Integer deckId, Pageable pageable);

    Optional<Note> findByIdAndDeckId(Integer id, Integer deckId);
}
