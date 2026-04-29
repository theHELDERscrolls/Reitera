package com.helderruiz.reitera_backend.modules.study.model;

import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable ledger recording every review action performed by a user on a card.
 * Essential for FSRS algorithm optimization and user analytics.
 */
@Entity
@Table(name = "review_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /**
     * The grade given by the user: 1 (Again), 2 (Hard), 3 (Good), 4 (Easy).
     */
    @Column(nullable = false)
    private Integer rating;

    @Column(name = "elapsed_days", nullable = false)
    private Integer elapsedDays;

    @Column(name = "scheduled_days", nullable = false)
    private Integer scheduledDays;

    @CreationTimestamp
    @Column(name = "review_date", updatable = false)
    private LocalDateTime reviewDate;
}