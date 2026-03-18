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

    // --- FSRS-6 default parameters (21 values trained on large-scale review data) ---
    // w[0-3]:  initial stability per rating (S₀)
    // w[4-7]:  initial difficulty and its update formula (D₀, D')
    // w[8-11]: stability after a successful recall (S'_recall)
    // w[12-15]: stability after forgetting (S'_forget) + hard penalty
    // w[16-18]: easy bonus + same-day stability scaling
    // w[19-20]: same-day power + decay factor for the forgetting curve
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

    // Card state constants (matches the 'state' column in study_progress table)
    private static final int NEW = 0;
    private static final int LEARNING = 1;
    private static final int REVIEW = 2;
    private static final int RELEARNING = 3;

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // PRIVATE — SCHEDULING LOGIC
    // -------------------------------------------------------------------------

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
        // The initial stability depends entirely on how well the user knew the card
        // on first exposure. Rating 4 (Easy) gives ~8 days; Rating 1 (Again) gives ~0.2 days.
        double stability = initialStability(rating);

        // D₀(rating) = w[4] - exp(w[5] * (rating - 1)) + 1
        // Difficulty is high when the card was hard on first try, low when easy.
        double difficulty = initialDifficulty(rating);

        // Convert stability (in days) to a minute-precision interval.
        // Minimum 1 minute — avoids scheduling a card for the exact current moment.
        // For Again (S₀ = 0.212 days) this yields ~305 min; for Good (~2.3 days) ~3312 min.
        long scheduledMinutes = Math.max(1, nextIntervalMinutes(stability));

        // scheduledDays stores whole days for analytics; sub-day cards store 0.
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
                .state(LEARNING)                          // New → Learning
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

        // Elapsed time in whole days (stored in StudyProgress for analytics).
        int elapsedDays = (int) ChronoUnit.DAYS.between(progress.getLastReview(), now);

        // Elapsed time as a decimal fraction of days for accurate retrievability.
        // Using minutes avoids losing precision for sub-day intervals:
        // e.g. 305 minutes → 0.212 days, matching the S₀ that scheduled the card.
        double elapsedDaysDecimal = ChronoUnit.MINUTES.between(progress.getLastReview(), now) / 1440.0;

        // R(t, S) = (1 + factor * t/S) ^ (-w[20])
        // Retrievability: probability the user still remembers the card right now.
        // When t = scheduledDays, R ≈ 0.9 (our retention target).
        double retrievability = retrievability(elapsedDaysDecimal, progress.getStability());

        // D'(D, rating) = w[7] * D₀(4) + (1 - w[7]) * (D - w[6] * (rating - 3))
        // Difficulty drifts towards the mean based on the user's current rating.
        double newDifficulty = updateDifficulty(progress.getDifficulty(), rating);

        double newStability;
        int newState;
        int newLapses = progress.getLapses();

        if (rating == 1) {
            // Again — the user forgot the card
            // S'_forget uses a separate formula that gives a low but non-zero stability.
            newStability = stabilityAfterForgetting(newDifficulty, progress.getStability(), retrievability);
            newLapses = progress.getLapses() + 1;

            // Review → Relearning; Learning/Relearning → stays in Relearning
            newState = (progress.getState() == REVIEW) ? RELEARNING : RELEARNING;
        } else {
            // Hard / Good / Easy — the user recalled the card
            // S'_recall grows the stability; the amount depends on rating, D, S, and R.
            newStability = stabilityAfterRecall(newDifficulty, progress.getStability(), retrievability, rating);

            // State transitions on successful recall
            newState = switch (progress.getState()) {
                case LEARNING -> (rating >= 3) ? REVIEW : LEARNING;
                case RELEARNING -> (rating >= 3) ? REVIEW : RELEARNING;
                default -> REVIEW; // Already in Review, stays in Review
            };
        }

        // Minimum 1 minute — the formula naturally produces sub-day intervals
        // for low-stability cards (Again/Hard in Learning), so we allow them.
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

    // -------------------------------------------------------------------------
    // PRIVATE — FSRS-6 FORMULAS
    // -------------------------------------------------------------------------

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
     * 't' is expressed as decimal days (e.g. 0.212 for 305 minutes) to preserve
     * precision for sub-day intervals. R(S, S) = 0.9 by design.
     */
    private double retrievability(double elapsedDays, double stability) {
        double decay = -W[20];                             // w[20] = 0.1542
        double factor = Math.pow(0.9, 1.0 / decay) - 1;    // derived constant
        return Math.pow(1 + factor * elapsedDays / stability, decay);
    }

    /**
     * interval (minutes) = S_days * 1440 / factor * (requestRetention ^ (1/decay) - 1)
     * <p>
     * Converts the stability (expressed in days) to a minute-precision interval.
     * This allows FSRS to schedule sub-day reviews for low-stability cards
     * (e.g. Again on a new card → S₀ = 0.212 days → ~305 minutes).
     */
    private long nextIntervalMinutes(double stability) {
        double decay = -W[20];
        double factor = Math.pow(0.9, 1.0 / decay) - 1;
        double intervalDays = stability / factor * (Math.pow(REQUEST_RETENTION, 1.0 / decay) - 1);
        return Math.round(intervalDays * 1440); // convert days → minutes
    }

    /**
     * D'(D, rating) = w[7] * D₀(4) + (1 - w[7]) * (D - w[6] * (rating - 3))
     * <p>
     * Mean-reversion difficulty update. Each review nudges difficulty towards
     * the global mean (D₀ at rating=4). Clamped to [1, 10].
     */
    private double updateDifficulty(double difficulty, int rating) {
        double mean = initialDifficulty(4);                       // D₀(4) ≈ 4.93
        double d = W[7] * mean + (1 - W[7]) * (difficulty - W[6] * (rating - 3));
        return clampDifficulty(d);
    }

    /**
     * S'_recall = S * exp(w[8]) * (11 - D) * S^(-w[9])
     * * (exp(w[10] * (1 - R)) - 1)
     * * hardPenalty   (if rating = 2)
     * * easyBonus     (if rating = 4)
     * <p>
     * Stability growth after successfully recalling a card. Grows faster when:
     * - Card is easy (low D)
     * - Current stability is low (we learn more when we barely remember)
     * - Time elapsed was long (high forgetting = stronger encoding on recall)
     */
    private double stabilityAfterRecall(double difficulty,
                                        double stability,
                                        double retrievability,
                                        int rating) {

        double hardPenalty = (rating == 2) ? W[15] : 1.0;   // w[15] = 0.6014
        double easyBonus = (rating == 4) ? W[16] : 1.0;   // w[16] = 1.8729

        double newS = stability
                * Math.exp(W[8])
                * (11 - difficulty)
                * Math.pow(stability, -W[9])
                * (Math.exp(W[10] * (1 - retrievability)) - 1)
                * hardPenalty
                * easyBonus;

        // Stability must always increase on a successful recall
        return Math.max(newS, stability + 0.01);
    }

    /**
     * S'_forget = w[11] * D^(-w[12]) * ((S + 1)^w[13] - 1) * exp(w[14] * (1 - R))
     * <p>
     * Stability after forgetting (rating = 1, Again). The card re-enters learning
     * but retains some residual stability — it's not as hard as the very first time.
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
