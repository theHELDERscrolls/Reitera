package com.helderruiz.reitera_backend.modules.deck.repository;

import com.helderruiz.reitera_backend.modules.deck.model.UserDeckSubscription;
import com.helderruiz.reitera_backend.modules.deck.model.UserDeckSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeckSubscriptionRepository extends JpaRepository<UserDeckSubscription, UserDeckSubscriptionId> {
}