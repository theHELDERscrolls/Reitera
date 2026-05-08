package com.helderruiz.reitera_backend.modules.card.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.card.dto.CardRequestDTO;
import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.card.repository.CardSpecification;
import com.helderruiz.reitera_backend.modules.deck.dto.NewTagDTO;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.model.Tag;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.deck.repository.TagRepository;
import com.helderruiz.reitera_backend.modules.deck.service.TagService;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final TagService tagService;
    private final StudyProgressRepository studyProgressRepository;

    /**
     * Creates a new card inside a deck owned by the authenticated user.
     */
    @Transactional
    public CardResponseDTO createCard(Integer deckId, CardRequestDTO dto, User owner) {
        Deck deck = findOwnedDeck(deckId, owner);

        Set<Tag> tags = resolveTags(dto.tagIds(), dto.newTags(), owner);

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
     * Returns a paginated list of cards belonging to a deck owned by the authenticated user.
     * Each card is enriched with the user's FSRS state for that card (null if never studied).
     */
    public Page<CardResponseDTO> getCardsByDeck(Integer deckId, User owner, Pageable pageable) {
        Deck deck = findOwnedDeck(deckId, owner);

        Map<Integer, Integer> stateByCardId = studyProgressRepository
                .findAllByUserIdAndDeckId(owner.getId(), deckId)
                .stream()
                .collect(Collectors.toMap(
                        sp -> sp.getCard().getId(),
                        StudyProgress::getState
                ));

        return cardRepository.findAllByDeck(deck, pageable)
                .map(card -> toResponseDTO(card, stateByCardId.get(card.getId())));
    }

    /**
     * Returns a single card by ID, verifying deck ownership.
     */
    public CardResponseDTO getCardById(Integer deckId, Integer cardId, User owner) {
        findOwnedDeck(deckId, owner);
        Card card = findCardInDeck(cardId, deckId);
        return toResponseDTO(card);
    }

    /**
     * Updates an existing card. Only the deck owner can perform this operation.
     * After saving, orphaned tags (removed from this card and no longer used anywhere) are deleted.
     */
    @Transactional
    public CardResponseDTO updateCard(Integer deckId, Integer cardId, CardRequestDTO dto, User owner) {
        findOwnedDeck(deckId, owner);
        Card card = findCardInDeck(cardId, deckId);

        Set<Tag> oldTags = new HashSet<>(card.getTags());

        Set<Tag> newTags = resolveTags(dto.tagIds(), dto.newTags(), owner);
        card.setType(dto.type());
        card.setQuestion(dto.question());
        card.setAnswerJson(dto.answerJson());
        card.setExplanation(dto.explanation());
        card.setTags(newTags);

        CardResponseDTO result = toResponseDTO(cardRepository.save(card));

        cleanupOrphanTags(oldTags, newTags);

        return result;
    }

    /**
     * Deletes a card by ID. Only the deck owner can perform this operation.
     * After deletion, orphaned tags (no longer used anywhere) are deleted.
     */
    @Transactional
    public void deleteCard(Integer deckId, Integer cardId, User owner) {
        findOwnedDeck(deckId, owner);
        Card card = findCardInDeck(cardId, deckId);
        Set<Tag> cardTags = new HashSet<>(card.getTags());

        cardRepository.delete(card);

        cleanupOrphanTags(cardTags, Collections.emptySet());
    }

    /**
     * Returns a paginated list of ALL cards owned by the current user, across all their decks.
     * Accepts optional filters that are composed dynamically using JPA Specifications.
     * <p>
     * Filter parameters (all optional — null means "no filter"):
     * question — partial case-insensitive match on the card's question text
     * type     — exact match on card type (BASIC, MULTIPLE_CHOICE, TRUE_FALSE)
     * state    — FSRS study state: -1 = not yet studied, 0–3 = FSRS states
     * tagId    — must have this tag assigned
     * <p>
     * After fetching the page, each card is enriched with the user's study state
     * in a single batch query (avoids N+1).
     */
    public Page<CardResponseDTO> getAllCards(
            User owner, String question, String type, Integer state, Integer tagId, Pageable pageable) {

        Specification<Card> spec = Specification.where(CardSpecification.byOwner(owner.getId()));

        if (question != null && !question.isBlank()) {
            spec = spec.and(CardSpecification.questionContains(question));
        }
        if (type != null && !type.isBlank()) {
            spec = spec.and(CardSpecification.byType(type));
        }
        if (tagId != null) {
            spec = spec.and(CardSpecification.hasTag(tagId));
        }
        if (state != null) {
            if (state == -1) {
                spec = spec.and(CardSpecification.notStudied(owner.getId()));
            } else {
                spec = spec.and(CardSpecification.withState(state, owner.getId()));
            }
        }

        Page<Card> page = cardRepository.findAll(spec, pageable);

        List<Integer> cardIds = page.map(Card::getId).toList();
        Map<Integer, Integer> stateByCardId = studyProgressRepository
                .findAllByUserIdAndCardIdIn(owner.getId(), cardIds)
                .stream()
                .collect(Collectors.toMap(
                        sp -> sp.getCard().getId(),
                        StudyProgress::getState
                ));

        return page.map(card -> toResponseDTO(card, stateByCardId.get(card.getId())));
    }

    /**
     * Returns a paginated list of cards owned by the user that have the specified tag assigned.
     * Returns an empty page for both non-existent tags and tags with no matching cards
     * to prevent tag ID enumeration.
     */
    public Page<CardResponseDTO> getCardsByTag(Integer tagId, User owner, Pageable pageable) {
        return cardRepository.findByTagIdAndOwner(tagId, owner.getId(), pageable).map(this::toResponseDTO);
    }

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

        if (!card.getDeck().getId().equals(deckId)) {
            throw new ResourceNotFoundException("Card not found in the specified deck");
        }

        return card;
    }

    /**
     * Resolves the full set of Tag entities to assign to a card.
     * <p>
     * Two sources:
     * - tagIds: existing tags the user selected from the dropdown (verify they own them).
     * - newTags: name+color pairs typed by the user → find-or-create per user.
     * <p>
     * Ownership check on tagIds prevents IDOR: a user cannot attach another user's tag to their card.
     */
    private Set<Tag> resolveTags(Set<Integer> tagIds, List<NewTagDTO> newTags, User owner) {
        Set<Tag> result = new HashSet<>();

        if (tagIds != null) {
            tagIds.forEach(id -> {
                Tag tag = tagRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
                if (!tag.getOwner().getId().equals(owner.getId())) {
                    throw new ResourceNotFoundException("Tag not found with id: " + id);
                }
                result.add(tag);
            });
        }

        if (newTags != null) {
            newTags.forEach(dto -> result.add(tagService.findOrCreateTag(dto.name(), dto.hexColor(), owner)));
        }

        return result;
    }

    /**
     * Deletes tags that were removed from a card and are no longer used by any other card.
     * This is the same orphan-cleanup pattern used for categories in DeckService.
     * <p>
     * oldTags: tags the card had before the update/delete.
     * newTags: tags the card has after (empty set for deletes).
     */
    private void cleanupOrphanTags(Set<Tag> oldTags, Set<Tag> newTags) {
        Set<Integer> newTagIds = newTags.stream().map(Tag::getId).collect(Collectors.toSet());

        oldTags.stream()
                .filter(t -> !newTagIds.contains(t.getId()))
                .forEach(t -> {
                    if (cardRepository.countByTagsContaining(t) == 0) {
                        tagRepository.deleteById(t.getId());
                    }
                });
    }

    /**
     * Maps a Card entity to its response DTO.
     * Converts the Tag set into lightweight TagSummary records.
     * State is null for cards the user has never studied.
     */
    private CardResponseDTO toResponseDTO(Card card, Integer state) {
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
                tagSummaries,
                state
        );
    }

    /**
     * Overload for contexts where state is not available (single card lookups, create, update).
     * State is set to null — the caller is not in a deck-list context.
     */
    private CardResponseDTO toResponseDTO(Card card) {
        return toResponseDTO(card, null);
    }
}
