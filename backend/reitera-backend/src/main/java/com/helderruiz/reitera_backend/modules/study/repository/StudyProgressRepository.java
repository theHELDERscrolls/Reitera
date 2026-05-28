package com.helderruiz.reitera_backend.modules.study.repository;

import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

// Note: Optional import kept for potential future use even though unused currently

public interface StudyProgressRepository extends JpaRepository<StudyProgress, StudyProgressId> {

    /**
     * Returns all StudyProgress records for a user in a given deck
     * where the next review date is due (nextReview <= now).
     * <p>
     * These are cards the user has studied before and need to revisit today.
     * <p>
     * JPQL navigates relationships with dots: sp.card.deck.id accesses
     * StudyProgress → Card → Deck → id without needing a JOIN manually.
     */
    @Query("SELECT sp FROM StudyProgress sp " +
            "JOIN FETCH sp.card c " +
            "JOIN FETCH c.note " +
            "WHERE sp.user.id = :userId " +
            "AND c.deck.id = :deckId " +
            "AND sp.nextReview <= :now")
    List<StudyProgress> findDueByUserAndDeck(@Param("userId") UUID userId,
                                             @Param("deckId") Integer deckId,
                                             @Param("now") LocalDateTime now);

    /**
     * Returns due StudyProgress records for all decks the user owns within a category.
     * Navigates: StudyProgress → Card → Deck → Category and Deck → Owner.
     * Used when the user studies a full category (e.g. "Historia de España" → Tema 1 + Tema 2).
     */
    @Query("SELECT sp FROM StudyProgress sp " +
            "JOIN FETCH sp.card c " +
            "JOIN FETCH c.note " +
            "WHERE sp.user.id = :userId " +
            "AND c.deck.category.id = :categoryId " +
            "AND c.deck.owner.id = :userId " +
            "AND sp.nextReview <= :now")
    List<StudyProgress> findDueByUserAndCategory(@Param("userId") UUID userId,
                                                 @Param("categoryId") Integer categoryId,
                                                 @Param("now") LocalDateTime now);

    /**
     * Counts StudyProgress records for a user in a given deck that match a specific FSRS state.
     * Used to build the deck stats dashboard (learning / review / relearning counts).
     */
    @Query("SELECT COUNT(sp) FROM StudyProgress sp " +
            "WHERE sp.user.id = :userId " +
            "AND sp.card.deck.id = :deckId " +
            "AND sp.state = :state")
    long countByUserIdAndDeckIdAndState(@Param("userId") UUID userId,
                                        @Param("deckId") Integer deckId,
                                        @Param("state") Integer state);

    /**
     * Counts cards in a deck that are due for review right now (nextReview <= now).
     * Covers all states — a due card can be learning, review, or relearning.
     */
    @Query("SELECT COUNT(sp) FROM StudyProgress sp " +
            "WHERE sp.user.id = :userId " +
            "AND sp.card.deck.id = :deckId " +
            "AND sp.nextReview <= :now")
    long countDueByUserIdAndDeckId(@Param("userId") UUID userId,
                                   @Param("deckId") Integer deckId,
                                   @Param("now") LocalDateTime now);

    /**
     * Returns [deckId, state, count] tuples for all due progress records across a list of decks.
     * Used by the Study Hub to show per-deck counts in a single batch query instead of N+1.
     * Only states 1 (Learning), 2 (Review) and 3 (Relearning) are included — state 0 (New)
     * has no StudyProgress row and is counted separately via CardRepository.
     */
    @Query("SELECT sp.card.deck.id, sp.state, COUNT(sp) FROM StudyProgress sp " +
            "WHERE sp.user.id = :userId " +
            "AND sp.card.deck.id IN :deckIds " +
            "AND sp.nextReview <= :now " +
            "AND sp.state IN (1, 2, 3) " +
            "GROUP BY sp.card.deck.id, sp.state")
    List<Object[]> countDueByUserAndDeckIds(@Param("userId") UUID userId,
                                            @Param("deckIds") List<Integer> deckIds,
                                            @Param("now") LocalDateTime now);

    /**
     * Returns all StudyProgress records for a user across all cards in a given deck.
     * Used to enrich CardResponseDTO with per-card state without N+1 queries.
     */
    @Query("SELECT sp FROM StudyProgress sp " +
            "WHERE sp.user.id = :userId " +
            "AND sp.card.deck.id = :deckId")
    List<StudyProgress> findAllByUserIdAndDeckId(@Param("userId") UUID userId,
                                                 @Param("deckId") Integer deckId);

    /**
     * Returns StudyProgress records for a specific user and a list of card IDs.
     * Used by the cross-deck card list to enrich each card with its FSRS state
     * in a single batch query instead of one query per card (N+1 prevention).
     */
    @Query("SELECT sp FROM StudyProgress sp " +
            "WHERE sp.user.id = :userId " +
            "AND sp.card.id IN :cardIds")
    List<StudyProgress> findAllByUserIdAndCardIdIn(@Param("userId") UUID userId,
                                                   @Param("cardIds") List<Integer> cardIds);
}