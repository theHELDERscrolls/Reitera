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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public DeckResponseDTO createDeck(DeckRequestDTO dto, User owner) {
        Category category = findOrCreateCategory(dto.categoryId(), dto.categoryName());

        Deck deck = Deck.builder()
                .title(dto.title())
                .description(dto.description())
                .isPublic(dto.isPublic())
                .owner(owner)
                .authorName(owner.getNickname())
                .category(category)
                .build();

        return toResponseDTO(deckRepository.save(deck));
    }

    /**
     * Returns a paginated list of decks owned by the authenticated user.
     */
    public Page<DeckResponseDTO> getUserDecks(User owner, Pageable pageable) {
        return deckRepository.findAllByOwner(owner, pageable).map(this::toResponseDTO);
    }

    /**
     * Returns a single deck by ID, verifying the requester is the owner.
     */
    public DeckResponseDTO getDeckById(Integer id, User owner) {
        return toResponseDTO(findOwnedDeck(id, owner));
    }

    /**
     * Updates an existing deck. Only the owner can perform this operation.
     * If the category changes, the old category is checked for orphan status and deleted if unreferenced.
     */
    @Transactional
    public DeckResponseDTO updateDeck(Integer id, DeckRequestDTO dto, User owner) {
        Deck deck = findOwnedDeck(id, owner);

        // Keep a reference to the old category before overwriting it
        Category oldCategory = deck.getCategory();
        Category newCategory = findOrCreateCategory(dto.categoryId(), dto.categoryName());

        deck.setTitle(dto.title());
        deck.setDescription(dto.description());
        deck.setPublic(dto.isPublic());
        deck.setCategory(newCategory);

        DeckResponseDTO response = toResponseDTO(deckRepository.save(deck));

        // If the category changed, check whether the old one is now orphaned
        boolean categoryChanged = !isSameCategory(oldCategory, newCategory);
        if (categoryChanged) {
            deleteOrphanedCategory(oldCategory);
        }

        return response;
    }

    /**
     * Deletes a deck by ID. Only the owner can perform this operation.
     * After deletion, the former category is checked for orphan status and deleted if unreferenced.
     */
    @Transactional
    public void deleteDeck(Integer id, User owner) {
        Deck deck = findOwnedDeck(id, owner);
        Category formerCategory = deck.getCategory();

        deckRepository.delete(deck);

        deleteOrphanedCategory(formerCategory);
    }

    // --- Private helpers ---

    /**
     * Finds a deck by ID and verifies the requesting user is its owner.
     * Throws ResourceNotFoundException (404) in both cases — whether the deck does not exist
     * or belongs to a different user — to prevent leaking resource existence to potential attackers.
     */
    private Deck findOwnedDeck(Integer id, User owner) {
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found with id: " + id));

        if (!deck.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Deck not found with id: " + id);
        }

        return deck;
    }

    /**
     * Resolves the category to assign to a deck using a two-step strategy:
     *
     * 1. If categoryId is provided, fetch the existing category by ID (fails with 404 if not found).
     * 2. Otherwise, if categoryName is provided, search by name (case-insensitive).
     *    If a match is found it is reused; if not, a new Category row is created and saved.
     * 3. If neither is provided, returns null (deck without category is valid).
     *
     * categoryId takes precedence over categoryName when both are supplied.
     */
    private Category findOrCreateCategory(Integer categoryId, String categoryName) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        }

        if (categoryName != null && !categoryName.isBlank()) {
            return categoryRepository.findByNameIgnoreCase(categoryName.trim())
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder().name(categoryName.trim()).build()
                    ));
        }

        return null;
    }

    /**
     * Deletes the given category if it is no longer referenced by any deck.
     * Does nothing if the category is null (deck had no category).
     */
    private void deleteOrphanedCategory(Category category) {
        if (category == null) return;
        if (deckRepository.countByCategory(category) == 0) {
            categoryRepository.delete(category);
        }
    }

    /**
     * Returns true if both category references point to the same category (or are both null).
     * Used to avoid an unnecessary orphan check when the category did not change.
     */
    private boolean isSameCategory(Category a, Category b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.getId().equals(b.getId());
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
