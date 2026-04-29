package com.helderruiz.reitera_backend.modules.deck.model;

import com.helderruiz.reitera_backend.modules.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a user's subscription to a public deck.
 * Acts as a join table with additional metadata (subscription timestamp).
 */
@Entity
@Table(name = "user_deck_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeckSubscription {

    @EmbeddedId
    private UserDeckSubscriptionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("deckId")
    @JoinColumn(name = "deck_id")
    private Deck deck;

    @CreationTimestamp
    @Column(name = "subscribed_at", updatable = false)
    private LocalDateTime subscribedAt;
}