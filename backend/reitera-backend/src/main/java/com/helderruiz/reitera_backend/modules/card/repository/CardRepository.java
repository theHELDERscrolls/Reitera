package com.helderruiz.reitera_backend.modules.card.repository;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Integer> {
}