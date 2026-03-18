package com.helderruiz.reitera_backend.modules.deck.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckRequestDTO;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.model.Category;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.CategoryRepository;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core business logic for Deck management.
 * Enforces ownership rules: only the deck owner can modify or delete it.
 */
@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Creates a new deck and assigns it to the authenticated user.
     */
    public DeckResponseDTO createDeck(DeckRequestDTO dto, User owner) {
        // Resolve the optional category from the provided ID
        Category category = resolveCategory(dto.categoryId());

        // Build and persist the deck entity, setting the authenticated user as owner
        Deck deck = Deck.builder()
                .title(dto.title())
                .description(dto.description())
                .isPublic(dto.isPublic())
                .owner(owner)
                .authorName(owner.getFirstName() + " " + owner.getLastName())
                .category(category)
                .build();

        return toResponseDTO(deckRepository.save(deck));
    }

    /**
     * Returns all decks owned by the authenticated user.
     */
    public List<DeckResponseDTO> getUserDecks(User owner) {
        return deckRepository.findAllByOwner(owner).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Returns a single deck by ID, verifying the requester is the owner.
     */
    public DeckResponseDTO getDeckById(Integer id, User owner) {
        return toResponseDTO(findOwnedDeck(id, owner));
    }

    /**
     * Updates an existing deck. Only the owner can perform this operation.
     */
    public DeckResponseDTO updateDeck(Integer id, DeckRequestDTO dto, User owner) {
        Deck deck = findOwnedDeck(id, owner);

        // Apply updated values from the request DTO
        deck.setTitle(dto.title());
        deck.setDescription(dto.description());
        deck.setPublic(dto.isPublic());
        deck.setCategory(resolveCategory(dto.categoryId()));

        return toResponseDTO(deckRepository.save(deck));
    }

    /**
     * Deletes a deck by ID. Only the owner can perform this operation.
     */
    public void deleteDeck(Integer id, User owner) {
        deckRepository.delete(findOwnedDeck(id, owner));
    }

    // --- Private helpers ---

    /**
     * Finds a deck by ID and verifies the requesting user is its owner.
     * Throws ResourceNotFoundException (404) if not found.
     * Throws RuntimeException (400) if the user is not the owner.
     */
    private Deck findOwnedDeck(Integer id, User owner) {
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found with id: " + id));

        if (!deck.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("You do not have permission to access this deck");
        }

        return deck;
    }

    /**
     * Resolves a Category entity from an optional ID.
     * Returns null if no categoryId is provided (deck without category is valid).
     */
    private Category resolveCategory(Integer categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    /**
     * Maps a Deck entity to its response DTO.
     * Flattens the Category and Owner relationships into simple fields.
     */
    private DeckResponseDTO toResponseDTO(Deck deck) {
        return new DeckResponseDTO(
                deck.getId(),
                deck.getTitle(),
                deck.getDescription(),
                deck.isPublic(),
                deck.getAuthorName(),
                deck.getCategory() != null ? deck.getCategory().getId() : null,
                deck.getCategory() != null ? deck.getCategory().getName() : null,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}
