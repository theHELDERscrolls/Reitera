package com.helderruiz.reitera_backend.modules.deck.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckRequestDTO;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.model.Category;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.CategoryRepository;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.helderruiz.reitera_backend.modules.deck.dto.DeckStatsDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeckServiceTest {
    private User user;
    private Deck deck;
    private Category category;
    private Category newCategory;
    private Pageable pageable;

    @BeforeEach
    void setup() {
        user = User.builder().id(UUID.randomUUID()).build();
        category = Category.builder().id(1).build();
        deck = Deck.builder().id(1).category(category).owner(user).build();
        newCategory = Category.builder().id(2).build();
        pageable = PageRequest.of(0, 20);
    }

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private StudyProgressRepository studyProgressRepository;

    @InjectMocks
    private DeckService deckService;

    @Test
    void getDeckById_existingDeckWithMatchingOwner_returnsDeckResponseDTO() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));

        DeckResponseDTO result = deckService.getDeckById(deck.getId(), user);

        assertThat(result.title()).isEqualTo(deck.getTitle());
    }

    @Test
    void getDeckById_nonExistentDeck_throwsResourceNotFoundException() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getDeckById(deck.getId(), user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDeckById_deckBelongingToOtherUser_throwsResourceNotFoundException() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> deckService.getDeckById(deck.getId(), otherUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDeck_existingDeckWithOrphanedCategory_deletesDeckAndCategory() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(deckRepository.countByCategory(category)).thenReturn(0L);

        deckService.deleteDeck(deck.getId(), user);

        verify(deckRepository).delete(deck);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteDeck_existingDeckWithSharedCategory_deletesDeckButNotCategory() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(deckRepository.countByCategory(category)).thenReturn(1L);

        deckService.deleteDeck(deck.getId(), user);

        verify(deckRepository).delete(deck);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteDeck_existingDeckWithNoCategory_deletesDeckOnly() {
        Deck otherDeck = Deck.builder().id(1).category(null).owner(user).build();

        when(deckRepository.findById(otherDeck.getId())).thenReturn(Optional.of(otherDeck));

        deckService.deleteDeck(otherDeck.getId(), user);

        verify(deckRepository).delete(otherDeck);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteDeck_nonExistentDeck_throwsResourceNotFoundException() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.deleteDeck(deck.getId(), user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDeck_deckBelongingToOtherUser_throwsResourceNotFoundException() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> deckService.deleteDeck(deck.getId(), otherUser))
                .isInstanceOf(ResourceNotFoundException.class);

    }

    @Test
    void createDeck_withExistingCategoryId_returnsDeckResponseDTO() {
        DeckRequestDTO dto = new DeckRequestDTO("Test Deck", "Description", false, category.getId(), null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(deckRepository.save(any(Deck.class))).thenReturn(deck);

        DeckResponseDTO result = deckService.createDeck(dto, user);

        assertThat(result.title()).isEqualTo(deck.getTitle());
    }

    @Test
    void createDeck_withNewCategoryName_createsAndReturnsDeckResponseDTO() {
        DeckRequestDTO dto = new DeckRequestDTO("Test Deck", "Description", false, null, "New Category");

        when(categoryRepository.findByNameIgnoreCase("New Category")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(deckRepository.save(any(Deck.class))).thenReturn(deck);

        DeckResponseDTO result = deckService.createDeck(dto, user);

        assertThat(result.title()).isEqualTo(deck.getTitle());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createDeck_categoryIdNotFound_throwsResourceNotFoundException() {
        DeckRequestDTO dto = new DeckRequestDTO("Test Deck", "Description", false, 99, null);

        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.createDeck(dto, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDeck_categoryChangedToOrphan_deletesOldCategory() {
        DeckRequestDTO dto = new DeckRequestDTO("Updated", "Desc", false, newCategory.getId(), null);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
        when(deckRepository.save(any(Deck.class))).thenReturn(deck);
        when(deckRepository.countByCategory(category)).thenReturn(0L);

        deckService.updateDeck(deck.getId(), dto, user);

        verify(categoryRepository).delete(category);
    }

    @Test
    void updateDeck_categoryChangedToShared_keepsOldCategory() {
        DeckRequestDTO dto = new DeckRequestDTO("Updated", "Desc", false, newCategory.getId(), null);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
        when(deckRepository.save(any(Deck.class))).thenReturn(deck);
        when(deckRepository.countByCategory(category)).thenReturn(1L);

        deckService.updateDeck(deck.getId(), dto, user);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void updateDeck_sameCategoryId_doesNotCheckOrphan() {
        DeckRequestDTO dto = new DeckRequestDTO("Updated", "Desc", false, category.getId(), null);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(deckRepository.save(any(Deck.class))).thenReturn(deck);

        deckService.updateDeck(deck.getId(), dto, user);

        verify(deckRepository, never()).countByCategory(any());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void updateDeck_nonExistentDeck_throwsResourceNotFoundException() {
        DeckRequestDTO dto = new DeckRequestDTO("Updated", "Desc", false, null, null);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.updateDeck(deck.getId(), dto, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDeck_deckBelongingToOtherUser_throwsResourceNotFoundException() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        DeckRequestDTO dto = new DeckRequestDTO("Updated", "Desc", false, null, null);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> deckService.updateDeck(deck.getId(), dto, otherUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDeckStats_existingDeck_returnsStatsDTO() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(cardRepository.countByDeck(deck)).thenReturn(10L);
        when(cardRepository.countNewCardsByDeckAndUser(deck.getId(), user.getId())).thenReturn(3L);
        when(studyProgressRepository.countByUserIdAndDeckIdAndState(user.getId(), deck.getId(), 1)).thenReturn(2L);
        when(studyProgressRepository.countByUserIdAndDeckIdAndState(user.getId(), deck.getId(), 2)).thenReturn(4L);
        when(studyProgressRepository.countByUserIdAndDeckIdAndState(user.getId(), deck.getId(), 3)).thenReturn(1L);
        when(studyProgressRepository.countDueByUserIdAndDeckId(any(), any(), any(LocalDateTime.class))).thenReturn(5L);

        DeckStatsDTO result = deckService.getDeckStats(deck.getId(), user);

        assertThat(result.totalCards()).isEqualTo(10L);
        assertThat(result.newCards()).isEqualTo(3L);
        assertThat(result.learningCards()).isEqualTo(2L);
        assertThat(result.reviewCards()).isEqualTo(4L);
        assertThat(result.relearningCards()).isEqualTo(1L);
        assertThat(result.dueCards()).isEqualTo(5L);
    }

    @Test
    void getDeckStats_nonExistentDeck_throwsResourceNotFoundException() {
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getDeckStats(deck.getId(), user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserDecks_withCategoryFilter_returnsFilteredPage() {
        Page<Deck> page = new PageImpl<>(List.of(deck));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(deckRepository.findAllByOwnerAndCategory(user, category, pageable)).thenReturn(page);
        when(cardRepository.countNewCardsByDeckIdsAndUser(any(), any())).thenReturn(List.of());
        when(studyProgressRepository.countDueByUserAndDeckIds(any(), any(), any())).thenReturn(List.of());

        Page<DeckResponseDTO> result = deckService.getUserDecks(user, category.getId(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(deckRepository).findAllByOwnerAndCategory(user, category, pageable);
        verify(deckRepository, never()).findAllByOwner(any(), any());
    }

    @Test
    void getUserDecks_withoutFilter_returnsAllDecks() {
        Page<Deck> page = new PageImpl<>(List.of(deck));

        when(deckRepository.findAllByOwner(user, pageable)).thenReturn(page);
        when(cardRepository.countNewCardsByDeckIdsAndUser(any(), any())).thenReturn(List.of());
        when(studyProgressRepository.countDueByUserAndDeckIds(any(), any(), any())).thenReturn(List.of());

        Page<DeckResponseDTO> result = deckService.getUserDecks(user, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(deckRepository).findAllByOwner(user, pageable);
        verify(deckRepository, never()).findAllByOwnerAndCategory(any(), any(), any());
    }
}
