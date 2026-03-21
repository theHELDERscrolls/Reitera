package com.helderruiz.reitera_backend.modules.deck.repository;

import com.helderruiz.reitera_backend.modules.deck.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    /**
     * Returns the distinct tags used on cards belonging to decks owned by the given user.
     * Navigates Card → tags (ManyToMany) and Card → deck → owner.
     */
    @Query("SELECT DISTINCT t FROM Card c JOIN c.tags t WHERE c.deck.owner.id = :userId")
    List<Tag> findDistinctByCardOwner(@Param("userId") UUID userId);
}