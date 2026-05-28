package com.helderruiz.reitera_backend.modules.note.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.note.dto.NoteRequestDTO;
import com.helderruiz.reitera_backend.modules.note.dto.NoteResponseDTO;
import com.helderruiz.reitera_backend.modules.note.model.Note;
import com.helderruiz.reitera_backend.modules.note.repository.NoteRepository;
import com.helderruiz.reitera_backend.modules.note.service.NoteParser.CardData;
import com.helderruiz.reitera_backend.modules.note.service.NoteParser.NoteType;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final DeckRepository deckRepository;
    private final NoteParser parser;

    @Transactional
    public NoteResponseDTO createNote(Integer deckId, NoteRequestDTO dto, User owner) {
        Deck deck = findOwnedDeck(deckId, owner);

        NoteType type = parser.detectType(dto.content());

        if (type == NoteType.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Note content does not match any supported format. " +
                            "Use --- for BASIC, <-> for BASIC_REVERSE, {{c1::}} for CLOZE, " +
                            "or - [x]/- [ ] for MULTIPLE_CHOICE.");
        }

        if (parser.hasConflict(dto.content(), type)) {
            throw new IllegalArgumentException(
                    "Note content mixes markers from multiple types. " +
                            "A note must use only one type's syntax.");
        }

        Note note = Note.builder()
                .deck(deck)
                .type(type.name())
                .content(dto.content())
                .explanation(blankToNull(dto.explanation()))
                .build();

        noteRepository.save(note);
        buildCards(note, parser.parse(dto.content(), type), deck);

        return toResponseDTO(noteRepository.save(note));
    }

    public Page<NoteResponseDTO> getNotesByDeck(Integer deckId, User owner, Pageable pageable) {
        findOwnedDeck(deckId, owner);

        return noteRepository.findAllByDeckId(deckId, pageable).map(this::toResponseDTO);
    }

    public NoteResponseDTO getNoteById(Integer deckId, Integer noteId, User owner) {
        findOwnedDeck(deckId, owner);

        return toResponseDTO(findOwnedNote(deckId, noteId));
    }

    /**
     * Updates note content using a smart merge strategy:
     * - If the type did not change, existing cards are updated in place by key
     * (clozeIndex for CLOZE, ordinal for all others) so that study_progress is preserved.
     * - If the type changed, all child cards are replaced (FSRS state is lost intentionally
     * because the card semantics are now fundamentally different).
     */
    @Transactional
    public NoteResponseDTO updateNote(Integer deckId, Integer noteId, NoteRequestDTO dto, User owner) {
        findOwnedDeck(deckId, owner);
        Note note = findOwnedNote(deckId, noteId);

        NoteType newType = parser.detectType(dto.content());

        if (newType == NoteType.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Note content does not match any supported format.");
        }

        if (parser.hasConflict(dto.content(), newType)) {
            throw new IllegalArgumentException(
                    "Note content mixes markers from multiple types. " +
                            "A note must use only one type's syntax.");
        }

        note.setContent(dto.content());
        note.setExplanation(blankToNull(dto.explanation()));

        List<CardData> parsed = parser.parse(dto.content(), newType);

        if (!note.getType().equals(newType.name())) {
            note.getCards().clear();
            note.setType(newType.name());
            buildCards(note, parsed, note.getDeck());
        } else {
            smartMerge(note, parsed, newType);
        }

        return toResponseDTO(noteRepository.save(note));
    }

    @Transactional
    public void deleteNote(Integer deckId, Integer noteId, User owner) {
        findOwnedDeck(deckId, owner);
        Note note = findOwnedNote(deckId, noteId);
        note.getCards().clear();
        noteRepository.delete(note);
    }

    private void smartMerge(Note note, List<CardData> parsed, NoteType type) {
        boolean isCloze = type == NoteType.CLOZE;

        Map<Integer, Card> existingByKey = new HashMap<>();

        for (Card card : note.getCards()) {
            int key = isCloze
                    ? ((Number) card.getAnswerJson().get("clozeIndex")).intValue()
                    : card.getOrdinal();
            existingByKey.put(key, card);
        }

        List<Card> toKeep = new ArrayList<>();

        for (CardData data : parsed) {
            int key = isCloze
                    ? ((Number) data.answerJson().get("clozeIndex")).intValue()
                    : data.ordinal();

            Card card = existingByKey.remove(key);

            if (card != null) {
                card.setQuestion(data.question());
                card.setAnswerJson(data.answerJson());
                card.setOrdinal((short) data.ordinal());
                toKeep.add(card);
            } else {
                toKeep.add(buildCard(note, data, note.getDeck()));
            }
        }

        note.getCards().clear();
        note.getCards().addAll(toKeep);
    }

    private void buildCards(Note note, List<CardData> cardDataList, Deck deck) {
        for (CardData data : cardDataList) {
            note.getCards().add(buildCard(note, data, deck));
        }
    }

    private Card buildCard(Note note, CardData data, Deck deck) {
        return Card.builder()
                .note(note)
                .deck(deck)
                .type(note.getType())
                .ordinal((short) data.ordinal())
                .question(data.question())
                .answerJson(data.answerJson())
                .build();
    }

    private Deck findOwnedDeck(Integer deckId, User owner) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found with id: " + deckId));

        if (!deck.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Deck not found with id: " + deckId);
        }

        return deck;
    }

    private Note findOwnedNote(Integer deckId, Integer noteId) {
        return noteRepository.findByIdAndDeckId(noteId, deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + noteId));
    }

    private NoteResponseDTO toResponseDTO(Note note) {
        return new NoteResponseDTO(
                note.getId(),
                note.getDeck().getId(),
                note.getType(),
                note.getContent(),
                note.getExplanation(),
                note.getCards().size(),
                note.getCreatedAt()
        );
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
