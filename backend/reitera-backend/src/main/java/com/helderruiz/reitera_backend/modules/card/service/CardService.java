package com.helderruiz.reitera_backend.modules.card.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.card.dto.CardRequestDTO;
import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.model.Tag;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.deck.repository.TagRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core business logic for Card management.
 * Cards always belong to a Deck, so deck ownership is verified before every operation.
 */
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final TagRepository tagRepository;

    /**
     * Creates a new card inside a deck owned by the authenticated user.
     */
    public CardResponseDTO createCard(Integer deckId, CardRequestDTO dto, User owner) {
        Deck deck = findOwnedDeck(deckId, owner);

        // Load the Tag entities from the provided IDs (empty set if none provided)
        Set<Tag> tags = resolveTags(dto.tagIds());

        Card card = Card.builder()
                .deck(deck)
                .type(dto.type())
                .question(dto.question())
                .answerJson(dto.answerJson())
                .explanation(dto.explanation())
                .tags(tags)
                .build();

        return toResponseDTO(cardRepository.save(card));
    }

    /**
     * Returns all cards belonging to a deck owned by the authenticated user.
     */
    public List<CardResponseDTO> getCardsByDeck(Integer deckId, User owner) {
        Deck deck = findOwnedDeck(deckId, owner);
        return cardRepository.findAllByDeck(deck).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Returns a single card by ID, verifying deck ownership.
     */
    public CardResponseDTO getCardById(Integer deckId, Integer cardId, User owner) {
        findOwnedDeck(deckId, owner); // Ensure the deck belongs to the user
        Card card = findCardInDeck(cardId, deckId);
        return toResponseDTO(card);
    }

    /**
     * Updates an existing card. Only the deck owner can perform this operation.
     */
    public CardResponseDTO updateCard(Integer deckId, Integer cardId, CardRequestDTO dto, User owner) {
        findOwnedDeck(deckId, owner);
        Card card = findCardInDeck(cardId, deckId);

        // Apply updated values from the request DTO
        card.setType(dto.type());
        card.setQuestion(dto.question());
        card.setAnswerJson(dto.answerJson());
        card.setExplanation(dto.explanation());
        card.setTags(resolveTags(dto.tagIds()));

        return toResponseDTO(cardRepository.save(card));
    }

    /**
     * Deletes a card by ID. Only the deck owner can perform this operation.
     */
    public void deleteCard(Integer deckId, Integer cardId, User owner) {
        findOwnedDeck(deckId, owner);
        cardRepository.delete(findCardInDeck(cardId, deckId));
    }

    // --- Private helpers ---

    /**
     * Finds a deck by ID and verifies the requesting user is its owner.
     * Throws ResourceNotFoundException (404) in both cases — whether the deck does not exist
     * or belongs to a different user — to prevent leaking resource existence to potential attackers.
     */
    private Deck findOwnedDeck(Integer deckId, User owner) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found with id: " + deckId));

        if (!deck.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Deck not found with id: " + deckId);
        }

        return deck;
    }

    /**
     * Finds a card by ID and verifies it belongs to the expected deck.
     */
    private Card findCardInDeck(Integer cardId, Integer deckId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        // Extra safety check: ensure the card actually belongs to the declared deck
        if (!card.getDeck().getId().equals(deckId)) {
            throw new ResourceNotFoundException("Card " + cardId + " does not belong to deck " + deckId);
        }

        return card;
    }

    /**
     * Loads Tag entities from a set of IDs.
     * Returns an empty set if no IDs are provided.
     */
    private Set<Tag> resolveTags(Set<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return Collections.emptySet();
        return tagIds.stream()
                .map(id -> tagRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id)))
                .collect(Collectors.toSet());
    }

    /**
     * Maps a Card entity to its response DTO.
     * Converts the Tag set into lightweight TagSummary records.
     */
    private CardResponseDTO toResponseDTO(Card card) {
        Set<CardResponseDTO.TagSummary> tagSummaries = card.getTags().stream()
                .map(tag -> new CardResponseDTO.TagSummary(tag.getId(), tag.getName(), tag.getHexColor()))
                .collect(Collectors.toSet());

        return new CardResponseDTO(
                card.getId(),
                card.getDeck().getId(),
                card.getType(),
                card.getQuestion(),
                card.getAnswerJson(),
                card.getExplanation(),
                tagSummaries
        );
    }
}
