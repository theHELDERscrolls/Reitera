package com.helderruiz.reitera_backend.modules.study.model;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity tracking the Spaced Repetition (FSRS) metrics for a specific user and card.
 * Acts as the intermediate table between Users and Cards.
 */
@Entity
@Table(name = "study_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyProgress {

    @EmbeddedId
    private StudyProgressId id;

    // Maps the 'userId' from the composite key to the actual User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    // Maps the 'cardId' from the composite key to the actual Card entity
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cardId")
    @JoinColumn(name = "card_id")
    private Card card;

    // --- FSRS ALGORITHM CORE VARIABLES ---

    @Builder.Default
    @Column(nullable = false)
    private Double stability = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double difficulty = 0.0;

    @Builder.Default
    @Column(name = "elapsed_days", nullable = false)
    private Integer elapsedDays = 0;

    @Builder.Default
    @Column(name = "scheduled_days", nullable = false)
    private Integer scheduledDays = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer reps = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer lapses = 0;

    /**
     * FSRS State: 0 = New, 1 = Learning, 2 = Review, 3 = Relearning.
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer state = 0;

    @Column(name = "last_review")
    private LocalDateTime lastReview;

    @Column(name = "next_review")
    private LocalDateTime nextReview;
}