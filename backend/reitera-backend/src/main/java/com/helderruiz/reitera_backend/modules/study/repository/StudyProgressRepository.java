package com.helderruiz.reitera_backend.modules.study.repository;

import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
            "WHERE sp.user.id = :userId " +
            "AND sp.card.deck.id = :deckId " +
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
            "WHERE sp.user.id = :userId " +
            "AND sp.card.deck.category.id = :categoryId " +
            "AND sp.card.deck.owner.id = :userId " +
            "AND sp.nextReview <= :now")
    List<StudyProgress> findDueByUserAndCategory(@Param("userId") UUID userId,
                                                 @Param("categoryId") Integer categoryId,
                                                 @Param("now") LocalDateTime now);
}