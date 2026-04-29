package com.helderruiz.reitera_backend.modules.study.service;

import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgressId;
import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Pure implementation of the FSRS-6 spaced repetition algorithm.
 * Computes the next review date for a card based on the user's rating.
 * <p>
 * Reference: https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm
 * <p>
 * This service contains only mathematical logic — no database access.
 * That separation makes it easy to unit-test without mocking repositories.
 */
@Service
public class FsrsService {

    // w[0-3]:  initial stability per rating (S₀)
    // w[4-7]:  initial difficulty and its update formula (D₀, D')
    // w[8-15]: stability after recall (S'_recall) and after forgetting (S'_forget)
    // w[16-20]: easy bonus, same-day scaling, decay factor for the forgetting curve
    private static final double[] W = {
            0.212, 1.2931, 2.3065, 8.2956,
            6.4133, 0.8334, 3.0194, 0.001,
            1.8722, 0.1666, 0.796, 1.4835,
            0.0614, 0.2629, 1.6483, 0.6014,
            1.8729, 0.5425, 0.0912,
            0.0658, 0.1542
    };

    // Target retention: the algorithm schedules cards so that when they are due,
    // the user has a 90% probability of remembering them.
    private static final double REQUEST_RETENTION = 0.9;

    private static final int NEW = 0;
    private static final int LEARNING = 1;
    private static final int REVIEW = 2;
    private static final int RELEARNING = 3;

    /**
     * Main entry point. Given a card's current progress and the user's rating,
     * returns an updated StudyProgress with new FSRS values and next review date.
     * <p>
     * If progress is null, the card has never been studied — we initialize it.
     *
     * @param progress existing StudyProgress, or null for a brand-new card
     * @param rating   user rating: 1=Again, 2=Hard, 3=Good, 4=Easy
     * @param user     the authenticated user
     * @param card     the card being reviewed
     * @param now      current timestamp (injected so the algorithm is testable)
     * @return updated StudyProgress ready to be persisted
     */
    public StudyProgress schedule(StudyProgress progress,
                                  int rating,
                                  User user,
                                  Card card,
                                  LocalDateTime now) {

        if (progress == null || progress.getState() == NEW) {
            return scheduleNewCard(progress, rating, user, card, now);
        }

        return scheduleExistingCard(progress, rating, now);
    }

    /**
     * Handles the very first review of a card (state = New).
     * Computes initial stability S₀ and initial difficulty D₀ from the rating.
     * Transitions state from New → Learning.
     */
    private StudyProgress scheduleNewCard(StudyProgress existing,
                                          int rating,
                                          User user,
                                          Card card,
                                          LocalDateTime now) {

        // S₀(rating) = w[rating - 1]
        double stability = initialStability(rating);

        // D₀(rating) = w[4] - exp(w[5] * (rating - 1)) + 1
        double difficulty = initialDifficulty(rating);

        // Minimum 1 minute — for Again (S₀ = 0.212 days) this yields ~305 min.
        long scheduledMinutes = Math.max(1, nextIntervalMinutes(stability));
        int scheduledDays = (int) (scheduledMinutes / 1440);

        StudyProgressId id = (existing != null)
                ? existing.getId()
                : new StudyProgressId(user.getId(), card.getId());

        return StudyProgress.builder()
                .id(id)
                .user(user)
                .card(card)
                .stability(stability)
                .difficulty(difficulty)
                .elapsedDays(0)
                .scheduledDays(scheduledDays)
                .reps(1)
                .lapses(0)
                .state(LEARNING)
                .lastReview(now)
                .nextReview(now.plusMinutes(scheduledMinutes))
                .build();
    }

    /**
     * Handles a review for a card already in Learning, Review, or Relearning state.
     * Computes elapsed days, retrievability R, then updates D and S accordingly.
     */
    private StudyProgress scheduleExistingCard(StudyProgress progress,
                                               int rating,
                                               LocalDateTime now) {

        int elapsedDays = (int) ChronoUnit.DAYS.between(progress.getLastReview(), now);

        // Decimal days preserve sub-day precision: e.g. 305 min → 0.212 days.
        double elapsedDaysDecimal = ChronoUnit.MINUTES.between(progress.getLastReview(), now) / 1440.0;

        // R(t, S) = (1 + factor * t/S) ^ (-w[20])
        double retrievability = retrievability(elapsedDaysDecimal, progress.getStability());

        // D'(D, rating) = w[7] * D₀(4) + (1 - w[7]) * (D - w[6] * (rating - 3))
        double newDifficulty = updateDifficulty(progress.getDifficulty(), rating);

        double newStability;
        int newState;
        int newLapses = progress.getLapses();

        if (rating == 1) {
            newStability = stabilityAfterForgetting(newDifficulty, progress.getStability(), retrievability);
            newLapses = progress.getLapses() + 1;
            newState = RELEARNING;
        } else {
            newStability = stabilityAfterRecall(newDifficulty, progress.getStability(), retrievability, rating);
            newState = switch (progress.getState()) {
                case LEARNING -> (rating >= 3) ? REVIEW : LEARNING;
                case RELEARNING -> (rating >= 3) ? REVIEW : RELEARNING;
                default -> REVIEW;
            };
        }

        long scheduledMinutes = Math.max(1, nextIntervalMinutes(newStability));
        int scheduledDays = (int) (scheduledMinutes / 1440);

        return StudyProgress.builder()
                .id(progress.getId())
                .user(progress.getUser())
                .card(progress.getCard())
                .stability(newStability)
                .difficulty(newDifficulty)
                .elapsedDays(elapsedDays)
                .scheduledDays(scheduledDays)
                .reps(progress.getReps() + 1)
                .lapses(newLapses)
                .state(newState)
                .lastReview(now)
                .nextReview(now.plusMinutes(scheduledMinutes))
                .build();
    }

    /**
     * S₀(rating) = w[rating - 1]
     * <p>
     * Initial stability for a brand-new card. The higher the rating on first
     * exposure, the longer the memory will last before the first scheduled review.
     */
    private double initialStability(int rating) {
        return W[rating - 1];
    }

    /**
     * D₀(rating) = w[4] - exp(w[5] * (rating - 1)) + 1
     * <p>
     * Initial difficulty. Cards answered with Again are harder (higher D),
     * cards answered with Easy start off easier (lower D). Clamped to [1, 10].
     */
    private double initialDifficulty(int rating) {
        double d = W[4] - Math.exp(W[5] * (rating - 1)) + 1;
        return clampDifficulty(d);
    }

    /**
     * R(t, S) = (1 + factor * t / S) ^ (-w[20])
     * where factor = 0.9^(-1/w[20]) - 1
     * <p>
     * Retrievability: probability of recall after 't' days given stability 'S'.
     * 't' is expressed as decimal days to preserve precision for sub-day intervals.
     * R(S, S) = 0.9 by design.
     */
    private double retrievability(double elapsedDays, double stability) {
        double decay = -W[20];
        double factor = Math.pow(0.9, 1.0 / decay) - 1;
        return Math.pow(1 + factor * elapsedDays / stability, decay);
    }

    /**
     * interval (minutes) = S_days * 1440 / factor * (requestRetention ^ (1/decay) - 1)
     * <p>
     * Converts stability (in days) to a minute-precision interval, enabling
     * sub-day reviews for low-stability cards (e.g. Again → ~305 minutes).
     */
    private long nextIntervalMinutes(double stability) {
        double decay = -W[20];
        double factor = Math.pow(0.9, 1.0 / decay) - 1;
        double intervalDays = stability / factor * (Math.pow(REQUEST_RETENTION, 1.0 / decay) - 1);
        return Math.round(intervalDays * 1440);
    }

    /**
     * D'(D, rating) = w[7] * D₀(4) + (1 - w[7]) * (D - w[6] * (rating - 3))
     * <p>
     * Mean-reversion difficulty update. Each review nudges difficulty towards
     * the global mean (D₀ at rating=4). Clamped to [1, 10].
     */
    private double updateDifficulty(double difficulty, int rating) {
        double mean = initialDifficulty(4);
        double d = W[7] * mean + (1 - W[7]) * (difficulty - W[6] * (rating - 3));
        return clampDifficulty(d);
    }

    /**
     * S'_recall = S * exp(w[8]) * (11 - D) * S^(-w[9])
     * * (exp(w[10] * (1 - R)) - 1) * hardPenalty * easyBonus
     * <p>
     * Stability growth after successfully recalling a card. Grows faster when
     * the card is easy, current stability is low, or time elapsed was long.
     */
    private double stabilityAfterRecall(double difficulty,
                                        double stability,
                                        double retrievability,
                                        int rating) {

        double hardPenalty = (rating == 2) ? W[15] : 1.0;
        double easyBonus = (rating == 4) ? W[16] : 1.0;

        double newS = stability
                * Math.exp(W[8])
                * (11 - difficulty)
                * Math.pow(stability, -W[9])
                * (Math.exp(W[10] * (1 - retrievability)) - 1)
                * hardPenalty
                * easyBonus;

        return Math.max(newS, 0.01);
    }

    /**
     * S'_forget = w[11] * D^(-w[12]) * ((S + 1)^w[13] - 1) * exp(w[14] * (1 - R))
     * <p>
     * Stability after forgetting (rating = 1). The card re-enters learning
     * but retains some residual stability — it is not as hard as the very first time.
     */
    private double stabilityAfterForgetting(double difficulty,
                                            double stability,
                                            double retrievability) {

        return W[11]
                * Math.pow(difficulty, -W[12])
                * (Math.pow(stability + 1, W[13]) - 1)
                * Math.exp(W[14] * (1 - retrievability));
    }

    /**
     * Clamps difficulty to the valid FSRS range [1, 10].
     */
    private double clampDifficulty(double d) {
        return Math.max(1.0, Math.min(10.0, d));
    }
}
