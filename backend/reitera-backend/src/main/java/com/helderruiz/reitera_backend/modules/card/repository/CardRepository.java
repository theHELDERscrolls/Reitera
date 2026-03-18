package com.helderruiz.reitera_backend.modules.card.repository;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Integer> {

    // Returns all cards belonging to a specific deck
    List<Card> findAllByDeck(Deck deck);
}