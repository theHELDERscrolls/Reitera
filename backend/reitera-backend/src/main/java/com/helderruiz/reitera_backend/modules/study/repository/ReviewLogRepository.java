package com.helderruiz.reitera_backend.modules.study.repository;

import com.helderruiz.reitera_backend.modules.study.model.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Integer> {

    /**
     * Counts distinct cards reviewed by the user within a time window.
     * Used to compute the "studied today" dashboard stat.
     */
    @Query("SELECT COUNT(DISTINCT rl.card.id) FROM ReviewLog rl " +
            "WHERE rl.user.id = :userId " +
            "AND rl.reviewDate >= :start AND rl.reviewDate < :end")
    long countDistinctCardsReviewedBetween(@Param("userId") UUID userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    /**
     * Returns distinct calendar dates on which the user reviewed at least one card,
     * from the given point in time onwards. Results are ordered descending (most recent first).
     * Used to compute the study streak by walking backwards through consecutive days.
     */
    @Query(value = "SELECT DISTINCT DATE(review_date) FROM review_logs " +
            "WHERE user_id = :userId AND review_date >= :since " +
            "ORDER BY 1 DESC",
            nativeQuery = true)
    List<LocalDate> findDistinctReviewDatesSince(@Param("userId") UUID userId,
                                                 @Param("since") LocalDateTime since);

    /**
     * Returns the count of distinct cards reviewed per calendar day since the given
     * timestamp. Only days with at least one review are included in the result set.
     * Used to populate the activity heatmap on the stats page.
     *
     * @param userId the ID of the user whose review logs are queried
     * @param since  the earliest timestamp to include (inclusive)
     * @return list of {@code [date, count]} pairs ordered by date ascending
     */
    @Query(value = "SELECT DATE(review_date) AS review_day, COUNT(DISTINCT card_id) AS card_count " +
            "FROM review_logs " +
            "WHERE user_id = :userId AND review_date >= :since " +
            "GROUP BY DATE(review_date) " +
            "ORDER BY review_day",
            nativeQuery = true)
    List<Object[]> countDistinctCardsPerDaySince(@Param("userId") UUID userId,
                                                 @Param("since") LocalDateTime since);

    /**
     * Returns the last {@code limit} distinct deck IDs the user has reviewed, ordered by
     * most recent review date descending. Each row contains {@code [deck_id, last_reviewed]}.
     * Used by the dashboard "Last decks studied" panel; the limit is parameterised so
     * a future detail endpoint can request more rows without duplicating the query.
     */
    @Query(value = "SELECT c.deck_id, MAX(rl.review_date) AS last_reviewed " +
            "FROM review_logs rl " +
            "JOIN cards c ON rl.card_id = c.id " +
            "WHERE rl.user_id = :userId " +
            "GROUP BY c.deck_id " +
            "ORDER BY last_reviewed DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findLastStudiedDeckIds(@Param("userId") UUID userId, @Param("limit") int limit);

}