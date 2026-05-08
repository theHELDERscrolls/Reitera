package com.helderruiz.reitera_backend.modules.deck.repository;

import com.helderruiz.reitera_backend.modules.deck.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    /**
     * Returns all tags owned by the given user.
     * Used to populate the tag suggestions dropdown in the card form.
     */
    List<Tag> findAllByOwnerId(UUID ownerId);

    /**
     * Finds a tag by name (case-insensitive) for a specific owner.
     * Used for find-or-create: if the user types "España" and it already exists, reuse it.
     */
    Optional<Tag> findByNameIgnoreCaseAndOwnerId(String name, UUID ownerId);
}