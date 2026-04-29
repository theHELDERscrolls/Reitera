package com.helderruiz.reitera_backend.modules.deck.service;

import com.helderruiz.reitera_backend.modules.deck.dto.TagResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.model.Tag;
import com.helderruiz.reitera_backend.modules.deck.repository.TagRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Tag operations scoped to the authenticated user.
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /**
     * Returns all tags owned by this user.
     * Used to populate the tag suggestions dropdown in the card form.
     */
    public List<TagResponseDTO> getMyTags(User user) {
        return tagRepository.findAllByOwnerId(user.getId()).stream()
                .map(t -> new TagResponseDTO(t.getId(), t.getName(), t.getHexColor()))
                .toList();
    }

    /**
     * Finds an existing tag by name (case-insensitive) for this user, or creates a new one.
     * This is the same find-or-create pattern used for categories in DeckService.
     * If "España" already exists for this user, it is reused regardless of the provided color.
     */
    @Transactional
    public Tag findOrCreateTag(String name, String hexColor, User owner) {
        return tagRepository.findByNameIgnoreCaseAndOwnerId(name.trim(), owner.getId())
                .orElseGet(() -> tagRepository.save(
                        Tag.builder()
                                .name(name.trim())
                                .hexColor(hexColor)
                                .owner(owner)
                                .build()
                ));
    }
}
