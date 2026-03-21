package com.helderruiz.reitera_backend.modules.deck.repository;

import com.helderruiz.reitera_backend.modules.deck.model.Category;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, Integer> {

    // Returns all decks owned by a specific user (used for the "my decks" listing)
    List<Deck> findAllByOwner(User owner);

    /**
     * Returns the distinct categories used across all decks owned by the given user.
     * Decks without a category (category IS NULL) are excluded.
     */
    @Query("SELECT DISTINCT d.category FROM Deck d WHERE d.owner.id = :userId AND d.category IS NOT NULL")
    List<Category> findDistinctCategoriesByOwnerId(@Param("userId") UUID userId);
}