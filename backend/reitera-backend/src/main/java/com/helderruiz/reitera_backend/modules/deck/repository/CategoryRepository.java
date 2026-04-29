package com.helderruiz.reitera_backend.modules.deck.repository;

import com.helderruiz.reitera_backend.modules.deck.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * Finds a category by name ignoring case differences (e.g. "historia" matches "Historia").
     * Used by the find-or-create logic in DeckService.
     */
    Optional<Category> findByNameIgnoreCase(String name);
}