package com.helderruiz.reitera_backend.modules.card.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.card.dto.CardRequestDTO;
import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {
    private User user;
    private Deck deck;
    private Card card;
    private CardRequestDTO dto;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private StudyProgressRepository studyProgressRepository;

    @InjectMocks
    private CardService cardService;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        deck = Deck.builder().id(1).owner(user).build();
        card = Card.builder()
                .id(1)
                .deck(deck)
                .type("BASIC")
                .question("What is Java?")
                .build();
        dto = new CardRequestDTO(
                "BASIC",
                "What is Java?",
                Map.of("answer", "A language"),
                null);
    }

    @Test
    void getCardsByDeck_existingDeck_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(studyProgressRepository.findAllByUserIdAndDeckId(user.getId(), deck.getId())).thenReturn(List.of());
        when(cardRepository.findAllByDeck(deck, pageable)).thenReturn(new PageImpl<>(List.of(card)));

        Page<CardResponseDTO> result = cardService.getCardsByDeck(deck.getId(), user, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).question()).isEqualTo(card.getQuestion());
    }

    @Test
    void getCardsByDeck_deckNotFound_throwsResourceNotFoundException() {
        Pageable pageable = PageRequest.of(0, 20);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardsByDeck(deck.getId(), user, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCardById_existingCard_returnsCardResponseDTO() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        CardResponseDTO result = cardService.getCardById(deck.getId(), card.getId(), user);

        assertThat(result.deckId()).isEqualTo(deck.getId());
        assertThat(result.question()).isEqualTo(card.getQuestion());
    }

    @Test
    void getCardById_nonExistentCard_throwsResourceNotFoundException() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(cardRepository.findById(card.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardById(deck.getId(), card.getId(), user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createCard_returnsCardResponseDTO() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        CardResponseDTO result = cardService.createCard(deck.getId(), dto, user);

        assertThat(result.question()).isEqualTo(card.getQuestion());
    }

    @Test
    void createCard_deckNotFound_throwsResourceNotFoundException() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(deck.getId(), dto, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCardById_cardBelongingToOtherDeck_throwsResourceNotFoundException() {
        Deck otherDeck = Deck.builder().id(2).owner(user).build();
        Card cardInOtherDeck = Card.builder().id(1).deck(otherDeck).build();

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(cardRepository.findById(cardInOtherDeck.getId())).thenReturn(Optional.of(cardInOtherDeck));

        assertThatThrownBy(() -> cardService.getCardById(deck.getId(), cardInOtherDeck.getId(), user))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
