package com.helderruiz.reitera_backend.modules.card.repository;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, Integer> {

    // Returns all cards belonging to a specific deck
    List<Card> findAllByDeck(Deck deck);

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
            "WHERE c.deck.category.id = :categoryId " +
            "AND c.deck.owner.id = :userId " +
            "AND NOT EXISTS (" +
            "    SELECT sp FROM StudyProgress sp " +
            "    WHERE sp.card = c AND sp.user.id = :userId" +
            ")")
    List<Card> findNewCardsByCategoryAndUser(@Param("categoryId") Integer categoryId,
                                             @Param("userId") UUID userId);

    /**
     * Returns all cards owned by the user that have the specified tag assigned.
     * Navigates Card → tags (ManyToMany) and Card → deck → owner.
     */
    @Query("SELECT c FROM Card c JOIN c.tags t WHERE t.id = :tagId AND c.deck.owner.id = :userId")
    List<Card> findByTagIdAndOwner(@Param("tagId") Integer tagId, @Param("userId") UUID userId);
}