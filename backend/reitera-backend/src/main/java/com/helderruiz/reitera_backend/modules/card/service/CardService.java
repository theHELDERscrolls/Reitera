package com.helderruiz.reitera_backend.modules.card.service;

import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.card.repository.CardSpecification;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final StudyProgressRepository studyProgressRepository;

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
}
