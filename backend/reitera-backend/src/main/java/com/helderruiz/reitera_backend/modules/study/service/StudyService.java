package com.helderruiz.reitera_backend.modules.study.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.CategoryRepository;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.study.dto.CardRatingDTO;
import com.helderruiz.reitera_backend.modules.study.dto.DueCardDTO;
import com.helderruiz.reitera_backend.modules.study.dto.StudySessionRequestDTO;
import com.helderruiz.reitera_backend.modules.study.dto.StudySessionResponseDTO;
import com.helderruiz.reitera_backend.modules.study.model.ReviewLog;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgressId;
import com.helderruiz.reitera_backend.modules.study.repository.ReviewLogRepository;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the study session flow: fetching due cards and processing ratings.
 * Supports two study modes:
 * - Single deck:  study cards from one specific deck (deckId)
 * - Category:     study cards from all decks in a category (categoryId)
 * Delegates all FSRS scheduling math to FsrsService.
 */
@Service
@RequiredArgsConstructor
public class StudyService {

    private final DeckRepository deckRepository;
    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;
    private final StudyProgressRepository studyProgressRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final FsrsService fsrsService;

    /**
     * Returns all cards the user should study now.
     * Accepts either deckId (single deck) or categoryId (all decks in a category).
     * Exactly one must be provided — the other must be null.
     * <p>
     * Two groups are merged into one list:
     * 1. Cards with existing progress whose nextReview is in the past (overdue).
     * 2. Cards with no StudyProgress at all (brand-new cards).
     */
    @Transactional(readOnly = true)
    public List<DueCardDTO> getDueCards(Integer deckId, Integer categoryId, User user) {
        validateScope(deckId, categoryId);

        LocalDateTime now = LocalDateTime.now();

        if (deckId != null) {
            return getDueCardsByDeck(deckId, user, now);
        } else {
            return getDueCardsByCategory(categoryId, user, now);
        }
    }

    /**
     * Processes all card ratings submitted at the end of a study session.
     * Accepts either deckId or categoryId to determine scope and validate ownership.
     * <p>
     * For each rated card:
     * - Verifies the card belongs to the declared scope (deck or category).
     * - Loads existing StudyProgress (null if the card is new to this user).
     * - Calls FsrsService to compute the updated FSRS values.
     * - Persists the updated StudyProgress.
     * - Writes an immutable ReviewLog entry for audit and analytics.
     * <p>
     * Cards that no longer exist or fail scope verification are silently skipped
     * rather than aborting the whole session. This supports the localStorage
     * recovery path where backed-up cardIds may reference deleted cards.
     * Rating 2 is still rejected eagerly as it signals a malformed request.
     */
    @Transactional
    public StudySessionResponseDTO processSession(StudySessionRequestDTO dto, User user) {
        validateScope(dto.deckId(), dto.categoryId());

        LocalDateTime now = LocalDateTime.now();
        int processedCount = 0;

        for (CardRatingDTO ratingDTO : dto.ratings()) {
            if (ratingDTO.rating() == 2) {
                throw new IllegalArgumentException(
                        "Rating 2 is not a valid rating. Use 1 (Forgotten) or 3 (Remembered).");
            }

            Optional<Card> cardOpt = cardRepository.findById(ratingDTO.cardId());

            if (cardOpt.isEmpty()) continue;

            Card card = cardOpt.get();

            try {
                verifyCardScope(card, dto.deckId(), dto.categoryId(), user);
            } catch (Exception e) {
                continue;
            }

            StudyProgressId progressId = new StudyProgressId(user.getId(), ratingDTO.cardId());
            StudyProgress existing = studyProgressRepository.findById(progressId).orElse(null);
            StudyProgress updated = fsrsService.schedule(existing, ratingDTO.rating(), user, card, now);

            studyProgressRepository.save(updated);

            ReviewLog log = ReviewLog.builder()
                    .user(user)
                    .card(card)
                    .rating(ratingDTO.rating())
                    .elapsedDays(updated.getElapsedDays())
                    .scheduledDays(updated.getScheduledDays())
                    .build();

            reviewLogRepository.save(log);

            processedCount++;
        }

        return new StudySessionResponseDTO(dto.deckId(), dto.categoryId(), processedCount);
    }

    /**
     * Fetches due and new cards for a single deck.
     * Verifies the user is the deck owner before querying.
     */
    private List<DueCardDTO> getDueCardsByDeck(Integer deckId, User user, LocalDateTime now) {
        findOwnedDeck(deckId, user);

        List<DueCardDTO> overdue = studyProgressRepository
                .findDueByUserAndDeck(user.getId(), deckId, now)
                .stream()
                .map(sp -> toDueCardDTO(sp.getCard(), sp.getState()))
                .toList();

        List<DueCardDTO> newCards = cardRepository
                .findNewCardsByDeckAndUser(deckId, user.getId())
                .stream()
                .map(card -> toDueCardDTO(card, 0))
                .toList();

        List<DueCardDTO> all = new ArrayList<>(overdue);
        all.addAll(newCards);
        return all;
    }

    /**
     * Fetches due and new cards across all decks the user owns in a category.
     * Verifies the category exists before querying.
     */
    private List<DueCardDTO> getDueCardsByCategory(Integer categoryId, User user, LocalDateTime now) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + categoryId));

        List<DueCardDTO> overdue = studyProgressRepository
                .findDueByUserAndCategory(user.getId(), categoryId, now)
                .stream()
                .map(sp -> toDueCardDTO(sp.getCard(), sp.getState()))
                .toList();

        List<DueCardDTO> newCards = cardRepository
                .findNewCardsByCategoryAndUser(categoryId, user.getId())
                .stream()
                .map(card -> toDueCardDTO(card, 0))
                .toList();

        List<DueCardDTO> all = new ArrayList<>(overdue);
        all.addAll(newCards);
        return all;
    }

    /**
     * Ensures exactly one of deckId or categoryId is provided.
     * Having both or neither would be ambiguous for the algorithm.
     */
    private void validateScope(Integer deckId, Integer categoryId) {
        if (deckId == null && categoryId == null) {
            throw new IllegalArgumentException("Either deckId or categoryId must be provided");
        }
        if (deckId != null && categoryId != null) {
            throw new IllegalArgumentException("Provide either deckId or categoryId, not both");
        }
    }

    /**
     * Verifies that a card belongs to the declared study scope and is owned by the user.
     * For deck scope: card's deck must match deckId and be owned by the user.
     * For category scope: card's deck must belong to the category and be owned by the user.
     */
    private void verifyCardScope(Card card, Integer deckId, Integer categoryId, User user) {
        Deck deck = card.getDeck();

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Card not found");
        }

        if (deckId != null && !deck.getId().equals(deckId)) {
            throw new IllegalArgumentException("Card does not belong to the specified deck");
        }

        if (categoryId != null && (deck.getCategory() == null || !deck.getCategory().getId().equals(categoryId))) {
            throw new IllegalArgumentException("Card does not belong to the specified category");
        }
    }

    /**
     * Finds a deck by ID and verifies the requesting user is its owner.
     */
    private Deck findOwnedDeck(Integer deckId, User user) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found with id: " + deckId));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Deck not found with id: " + deckId);
        }

        return deck;
    }

    private DueCardDTO toDueCardDTO(Card card, Integer state) {
        return new DueCardDTO(
                card.getId(),
                card.getDeck().getId(),
                card.getType(),
                card.getQuestion(),
                card.getAnswerJson(),
                card.getExplanation(),
                state
        );
    }
}
