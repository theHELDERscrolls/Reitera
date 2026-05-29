package com.helderruiz.reitera_backend.modules.note.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.note.dto.NoteRequestDTO;
import com.helderruiz.reitera_backend.modules.note.dto.NoteResponseDTO;
import com.helderruiz.reitera_backend.modules.note.model.Note;
import com.helderruiz.reitera_backend.modules.note.repository.NoteRepository;
import com.helderruiz.reitera_backend.modules.note.service.NoteParser.NoteType;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final String BASIC_CONTENT = "What year did the war begin?\n\n---\n\n1936";

    private User owner;
    private User otherUser;
    private Deck deck;
    private Note note;
    private Note savedNote;
    private Pageable pageable;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private NoteParser parser;

    @InjectMocks
    private NoteService noteService;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(UUID.randomUUID()).build();
        otherUser = User.builder().id(UUID.randomUUID()).build();
        deck = Deck.builder().id(1).owner(owner).build();
        note = Note.builder()
                .id(1)
                .deck(deck)
                .type("BASIC")
                .content(BASIC_CONTENT)
                .cards(new ArrayList<>())
                .build();
        savedNote = Note.builder()
                .id(1)
                .deck(deck)
                .type("BASIC")
                .content(BASIC_CONTENT)
                .cards(new ArrayList<>())
                .build();
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void createNote_validBasicContent_returnsNoteResponseDTO() {
        NoteRequestDTO dto = new NoteRequestDTO(BASIC_CONTENT, null);

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(parser.detectType(BASIC_CONTENT)).thenReturn(NoteType.BASIC);
        when(parser.hasConflict(BASIC_CONTENT, NoteType.BASIC)).thenReturn(false);
        when(parser.parse(BASIC_CONTENT, NoteType.BASIC))
                .thenReturn(List.of(new NoteParser.CardData("What year did the war begin?", Map.of("answer", "1936"), 0)));
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        NoteResponseDTO result = noteService.createNote(1, dto, owner);

        assertThat(result.type()).isEqualTo("BASIC");
        assertThat(result.deckId()).isEqualTo(1);
        verify(noteRepository, times(2)).save(any(Note.class));
    }

    @Test
    void createNote_unknownContent_throwsIllegalArgumentException() {
        NoteRequestDTO dto = new NoteRequestDTO("plain text with no markers", null);

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(parser.detectType("plain text with no markers")).thenReturn(NoteType.UNKNOWN);

        assertThatThrownBy(() -> noteService.createNote(1, dto, owner))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createNote_conflictingMarkers_throwsIllegalArgumentException() {
        String conflictContent = "{{c1::answer}}\n- [x] Option";
        NoteRequestDTO dto = new NoteRequestDTO(conflictContent, null);

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(parser.detectType(conflictContent)).thenReturn(NoteType.CLOZE);
        when(parser.hasConflict(conflictContent, NoteType.CLOZE)).thenReturn(true);

        assertThatThrownBy(() -> noteService.createNote(1, dto, owner))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createNote_deckNotFound_throwsResourceNotFoundException() {
        NoteRequestDTO dto = new NoteRequestDTO(BASIC_CONTENT, null);

        when(deckRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(99, dto, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createNote_deckBelongingToOtherUser_throwsResourceNotFoundException() {
        NoteRequestDTO dto = new NoteRequestDTO(BASIC_CONTENT, null);

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> noteService.createNote(1, dto, otherUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateNote_changedType_replacesAllCards() {
        String mcContent = "Which side won?\n\n- [x] Bando Nacional\n- [ ] Republicanos";
        NoteRequestDTO dto = new NoteRequestDTO(mcContent, null);

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(noteRepository.findByIdAndDeckId(1, 1)).thenReturn(Optional.of(note));
        when(parser.detectType(mcContent)).thenReturn(NoteType.MULTIPLE_CHOICE);
        when(parser.hasConflict(mcContent, NoteType.MULTIPLE_CHOICE)).thenReturn(false);
        when(parser.parse(mcContent, NoteType.MULTIPLE_CHOICE))
                .thenReturn(List.of(new NoteParser.CardData("Which side won?", Map.of("options", List.of("Bando Nacional", "Republicanos"), "correctIndex", 0), 0)));
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        noteService.updateNote(1, 1, dto, owner);

        assertThat(note.getType()).isEqualTo("MULTIPLE_CHOICE");
    }

    @Test
    void updateNote_unknownContent_throwsIllegalArgumentException() {
        NoteRequestDTO dto = new NoteRequestDTO("plain text", null);

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(noteRepository.findByIdAndDeckId(1, 1)).thenReturn(Optional.of(note));
        when(parser.detectType("plain text")).thenReturn(NoteType.UNKNOWN);

        assertThatThrownBy(() -> noteService.updateNote(1, 1, dto, owner))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateNote_noteNotFound_throwsResourceNotFoundException() {
        NoteRequestDTO dto = new NoteRequestDTO(BASIC_CONTENT, null);

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(noteRepository.findByIdAndDeckId(99, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.updateNote(1, 99, dto, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteNote_existingNote_callsRepositoryDelete() {
        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(noteRepository.findByIdAndDeckId(1, 1)).thenReturn(Optional.of(note));

        noteService.deleteNote(1, 1, owner);

        verify(noteRepository).delete(note);
    }

    @Test
    void getNotesByDeck_existingDeck_returnsPage() {
        Page<Note> notePage = new PageImpl<>(List.of(note));

        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(noteRepository.findAllByDeckId(1, pageable)).thenReturn(notePage);

        Page<NoteResponseDTO> result = noteService.getNotesByDeck(1, owner, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().type()).isEqualTo("BASIC");
    }

    @Test
    void getNoteById_noteNotFound_throwsResourceNotFoundException() {
        when(deckRepository.findById(1)).thenReturn(Optional.of(deck));
        when(noteRepository.findByIdAndDeckId(99, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(1, 99, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
