package com.helderruiz.reitera_backend.modules.deck.service;

import com.helderruiz.reitera_backend.modules.deck.dto.CategoryResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for Category queries.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final DeckRepository deckRepository;

    /**
     * Returns the distinct categories used across all decks owned by the authenticated user.
     */
    public List<CategoryResponseDTO> getMyCategories(User user) {
        return deckRepository.findDistinctCategoriesByOwnerId(user.getId()).stream()
                .map(c -> new CategoryResponseDTO(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }
}
