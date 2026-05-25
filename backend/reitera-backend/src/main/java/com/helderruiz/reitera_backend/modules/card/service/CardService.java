package com.helderruiz.reitera_backend.modules.card.service;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.card.dto.CardRequestDTO;
import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.card.repository.CardSpecification;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final StudyProgressRepository studyProgressRepository;

    @Transactional
    public CardResponseDTO createCard(Integer deckId, CardRequestDTO dto, User owner) {
        Deck deck = findOwnedDeck(deckId, owner);

        Card card = Card.builder()
                .deck(deck)
                .type(dto.type())
                .question(dto.question())
                .answerJson(dto.answerJson())
                .explanation(dto.explanation())
                .build();

        return toResponseDTO(cardRepository.save(card));
    }

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

    public CardResponseDTO getCardById(Integer deckId, Integer cardId, User owner) {
        findOwnedDeck(deckId, owner);
        Card card = findCardInDeck(cardId, deckId);
        return toResponseDTO(card);
    }

    @Transactional
    public CardResponseDTO updateCard(Integer deckId, Integer cardId, CardRequestDTO dto, User owner) {
        findOwnedDeck(deckId, owner);
        Card card = findCardInDeck(cardId, deckId);

        card.setType(dto.type());
        card.setQuestion(dto.question());
        card.setAnswerJson(dto.answerJson());
        card.setExplanation(dto.explanation());

        return toResponseDTO(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Integer deckId, Integer cardId, User owner) {
        findOwnedDeck(deckId, owner);
        Card card = findCardInDeck(cardId, deckId);
        cardRepository.delete(card);
    }

    /**
     * Returns a paginated list of ALL cards owned by the current user, across all their decks.
     * Accepts optional filters: question (partial match), type (exact), state (FSRS or -1 for new).
     */
    public Page<CardResponseDTO> getAllCards(
            User owner, String question, String type, Integer state, Pageable pageable) {

        Specification<Card> spec = Specification.where(CardSpecification.byOwner(owner.getId()));

        if (question != null && !question.isBlank()) {
            spec = spec.and(CardSpecification.questionContains(question));
        }
        if (type != null && !type.isBlank()) {
            spec = spec.and(CardSpecification.byType(type));
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

    private Deck findOwnedDeck(Integer deckId, User owner) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found with id: " + deckId));

        if (!deck.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Deck not found with id: " + deckId);
        }

        return deck;
    }

    private Card findCardInDeck(Integer cardId, Integer deckId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (!card.getDeck().getId().equals(deckId)) {
            throw new ResourceNotFoundException("Card not found in the specified deck");
        }

        return card;
    }

    private CardResponseDTO toResponseDTO(Card card, Integer state) {
        return new CardResponseDTO(
                card.getId(),
                card.getDeck().getId(),
                card.getType(),
                card.getQuestion(),
                card.getAnswerJson(),
                card.getExplanation(),
                state
        );
    }

    private CardResponseDTO toResponseDTO(Card card) {
        return toResponseDTO(card, null);
    }
}
