package com.helderruiz.reitera_backend.modules.card.repository;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, Integer>, JpaSpecificationExecutor<Card> {

    Page<Card> findAllByDeck(Deck deck, Pageable pageable);

    long countByDeck(Deck deck);

    /**
     * Returns [deckId, count] tuples for new cards (no StudyProgress) across a list of decks.
     * Used by the Study Hub to show per-deck new-card counts in one batch query.
     */
    @Query("SELECT c.deck.id, COUNT(c) FROM Card c " +
            "WHERE c.deck.id IN :deckIds " +
            "AND NOT EXISTS (" +
            "    SELECT sp FROM StudyProgress sp " +
            "    WHERE sp.card = c AND sp.user.id = :userId" +
            ") " +
            "GROUP BY c.deck.id")
    List<Object[]> countNewCardsByDeckIdsAndUser(@Param("deckIds") List<Integer> deckIds,
                                                 @Param("userId") UUID userId);

    /**
     * Counts cards in a deck that the user has never studied (no StudyProgress record).
     * Uses the same NOT EXISTS logic as findNewCardsByDeckAndUser but returns a scalar count.
     */
    @Query("SELECT COUNT(c) FROM Card c " +
            "WHERE c.deck.id = :deckId " +
            "AND NOT EXISTS (" +
            "    SELECT sp FROM StudyProgress sp " +
            "    WHERE sp.card = c AND sp.user.id = :userId" +
            ")")
    long countNewCardsByDeckAndUser(@Param("deckId") Integer deckId,
                                    @Param("userId") UUID userId);

    /**
     * Returns cards in a deck that the user has never studied before.
     * <p>
     * A card is considered "new" when there is no StudyProgress record
     * linking this user to this card. The NOT EXISTS subquery checks
     * the study_progress table for that combination.
     * <p>
     * These cards will be included in the due list alongside overdue cards.
     */
    @Query("SELECT c FROM Card c " +
            "JOIN FETCH c.note " +
            "WHERE c.deck.id = :deckId " +
            "AND NOT EXISTS (" +
            "    SELECT sp FROM StudyProgress sp " +
            "    WHERE sp.card = c AND sp.user.id = :userId" +
            ")")
    List<Card> findNewCardsByDeckAndUser(@Param("deckId") Integer deckId,
                                         @Param("userId") UUID userId);

    /**
     * Returns new cards across all decks owned by the user within a category.
     * Combines ownership (c.deck.owner.id), category filter, and NOT EXISTS
     * to exclude cards the user has already started studying.
     */
    @Query("SELECT c FROM Card c " +
            "JOIN FETCH c.note " +
            "WHERE c.deck.category.id = :categoryId " +
            "AND c.deck.owner.id = :userId " +
            "AND NOT EXISTS (" +
            "    SELECT sp FROM StudyProgress sp " +
            "    WHERE sp.card = c AND sp.user.id = :userId" +
            ")")
    List<Card> findNewCardsByCategoryAndUser(@Param("categoryId") Integer categoryId,
                                             @Param("userId") UUID userId);

}