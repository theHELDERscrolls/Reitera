package com.helderruiz.reitera_backend.modules.deck.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite Primary Key for UserDeckSubscription.
 * Ensures a user cannot subscribe to the same deck multiple times.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDeckSubscriptionId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "deck_id")
    private Integer deckId;
}