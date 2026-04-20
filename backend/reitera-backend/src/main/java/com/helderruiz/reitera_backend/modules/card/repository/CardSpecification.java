package com.helderruiz.reitera_backend.modules.card.repository;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Static factory methods that build JPA Specification predicates for Card queries.
 * <p>
 * Each method returns a Specification that can be combined with .and() in the service,
 * enabling flexible dynamic filtering without a single hardcoded @Query for every combination.
 * <p>
 * How Specification works:
 * - root     → the FROM clause entity (Card)
 * - query    → the full query object (used to create subqueries)
 * - builder  → the CriteriaBuilder, used to build predicates (=, LIKE, EXISTS, etc.)
 */
public class CardSpecification {

    private CardSpecification() {
    }

    /**
     * Restricts results to cards whose deck is owned by the given user.
     * Navigates: Card → deck → owner → id
     * This predicate is always applied — it is the security boundary.
     */
    public static Specification<Card> byOwner(UUID userId) {
        return (root, query, builder) ->
                builder.equal(root.get("deck").get("owner").get("id"), userId);
    }

    /**
     * Filters cards whose question contains the given string (case-insensitive).
     * SQL equivalent: LOWER(question) LIKE '%q%'
     */
    public static Specification<Card> questionContains(String question) {
        return (root, query, builder) ->
                builder.like(builder.lower(root.get("question")), "%" + question.toLowerCase() + "%");
    }

    /**
     * Filters cards by their type (BASIC, MULTIPLE_CHOICE, TRUE_FALSE).
     */
    public static Specification<Card> byType(String type) {
        return (root, query, builder) ->
                builder.equal(root.get("type"), type);
    }

    /**
     * Filters cards that have a specific tag assigned.
     * Uses EXISTS with a subquery rather than a JOIN to avoid duplicate rows
     * when a card has multiple tags.
     */
    public static Specification<Card> hasTag(Integer tagId) {
        return (root, query, builder) -> {
            var tagJoin = root.join("tags");

            return builder.equal(tagJoin.get("id"), tagId);
        };
    }

    /**
     * Filters cards that the user has NEVER studied.
     * A card is "not studied" when no StudyProgress row exists for this user+card pair.
     * SQL equivalent: NOT EXISTS (SELECT 1 FROM study_progress sp WHERE sp.card_id = card.id AND sp.user_id = userId)
     */
    public static Specification<Card> notStudied(UUID userId) {
        return (root, query, builder) -> {
            Subquery<StudyProgress> subquery = query.subquery(StudyProgress.class);
            var sp = subquery.from(StudyProgress.class);

            subquery.select(sp).where(
                    builder.equal(sp.get("card"), root),
                    builder.equal(sp.get("user").get("id"), userId)
            );

            return builder.not(builder.exists(subquery));
        };
    }

    /**
     * Filters cards that have a StudyProgress row with the specified FSRS state (0–3).
     * SQL equivalent: EXISTS (SELECT 1 FROM study_progress sp WHERE sp.card_id = card.id AND sp.user_id = userId AND sp.state = state)
     */
    public static Specification<Card> withState(int state, UUID userId) {
        return (root, query, builder) -> {
            Subquery<StudyProgress> subquery = query.subquery(StudyProgress.class);
            var sp = subquery.from(StudyProgress.class);

            subquery.select(sp).where(
                    builder.equal(sp.get("card"), root),
                    builder.equal(sp.get("user").get("id"), userId),
                    builder.equal(sp.get("state"), state)
            );

            return builder.exists(subquery);
        };
    }
}
