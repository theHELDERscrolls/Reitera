package com.helderruiz.reitera_backend.modules.deck.service;

import com.helderruiz.reitera_backend.modules.deck.dto.TagResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.repository.TagRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for Tag queries scoped to the authenticated user.
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /**
     * Returns the distinct tags used on cards belonging to the authenticated user's decks.
     */
    public List<TagResponseDTO> getMyTags(User user) {
        return tagRepository.findDistinctByCardOwner(user.getId()).stream()
                .map(t -> new TagResponseDTO(t.getId(), t.getName(), t.getHexColor()))
                .toList();
    }
}
