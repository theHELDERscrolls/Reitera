package com.helderruiz.reitera_backend.modules.study.service;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.CategoryRepository;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.study.dto.CardRatingDTO;
import com.helderruiz.reitera_backend.modules.study.dto.StudySessionRequestDTO;
import com.helderruiz.reitera_backend.modules.study.dto.StudySessionResponseDTO;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgressId;
import com.helderruiz.reitera_backend.modules.study.repository.ReviewLogRepository;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StudyServiceTest {

    private User user;
    private Deck deck;
    private Card card;
    private StudyProgress updatedProgress;
    private StudySessionRequestDTO dto;

    @BeforeEach
    void setup() {
        user = User.builder().id(UUID.randomUUID()).build();
        deck = Deck.builder().id(1).owner(user).build();
        card = Card.builder().id(1).deck(deck).tags(new HashSet<>()).build();
        updatedProgress = StudyProgress.builder().user(user).card(card).build();
        dto = new StudySessionRequestDTO(deck.getId(), null, List.of(new CardRatingDTO(card.getId(), 3)));
    }

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private StudyProgressRepository studyProgressRepository;

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @Mock
    private FsrsService fsrsService;

    @InjectMocks
    private StudyService studyService;

    @Test
    void processSession_withExistingProgress_savesUpdatedProgressAndLog() {
        StudyProgress existingProgress = StudyProgress.builder().user(user).card(card).state(2).reps(3).build();

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(studyProgressRepository.findById(new StudyProgressId(user.getId(), card.getId()))).thenReturn(Optional.of(existingProgress));
        when(fsrsService.schedule(any(), anyInt(), any(), any(), any())).thenReturn(updatedProgress);

        StudySessionResponseDTO result = studyService.processSession(dto, user);

        assertThat(result.cardsReviewed()).isEqualTo(1);
        verify(studyProgressRepository).save(updatedProgress);
        verify(reviewLogRepository).save(any());
    }

    @Test
    void processSession_withNewCard_createsProgressAndLog() {
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(studyProgressRepository.findById(new StudyProgressId(user.getId(), card.getId()))).thenReturn(Optional.empty());
        when(fsrsService.schedule(any(), anyInt(), any(), any(), any())).thenReturn(updatedProgress);

        StudySessionResponseDTO result = studyService.processSession(dto, user);

        assertThat(result.cardsReviewed()).isEqualTo(1);
        verify(studyProgressRepository).save(updatedProgress);
        verify(reviewLogRepository).save(any());
    }
}
