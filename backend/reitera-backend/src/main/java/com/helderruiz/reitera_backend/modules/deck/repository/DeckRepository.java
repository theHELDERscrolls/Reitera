package com.helderruiz.reitera_backend.modules.deck.repository;

import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeckRepository extends JpaRepository<Deck, Integer> {

    // Returns all decks owned by a specific user (used for the "my decks" listing)
    List<Deck> findAllByOwner(User owner);
}