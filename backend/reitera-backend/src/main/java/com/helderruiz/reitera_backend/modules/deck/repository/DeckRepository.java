package com.helderruiz.reitera_backend.modules.deck.repository;

import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, Integer> {
}