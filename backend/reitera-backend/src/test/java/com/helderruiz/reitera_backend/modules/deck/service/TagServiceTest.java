package com.helderruiz.reitera_backend.modules.deck.service;

import com.helderruiz.reitera_backend.modules.deck.dto.TagResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.model.Tag;
import com.helderruiz.reitera_backend.modules.deck.repository.TagRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {
    private User mockUser;
    private Tag existingTag;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(UUID.randomUUID()).build();
        existingTag = Tag.builder()
                .id(1)
                .name("Java")
                .hexColor("#f89820")
                .owner(mockUser)
                .build();
    }

    @Test
    void getMyTags_returnsMappedDTOs() {
        when(tagRepository.findAllByOwnerId(mockUser.getId())).thenReturn(List.of(existingTag));

        List<TagResponseDTO> result = tagService.getMyTags(mockUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(existingTag.getId());
        assertThat(result.get(0).name()).isEqualTo(existingTag.getName());
        assertThat(result.get(0).hexColor()).isEqualTo(existingTag.getHexColor());
    }

    @Test
    void findOrCreateTag_returnsExisting_whenFound() {
        when(tagRepository.findByNameIgnoreCaseAndOwnerId(existingTag.getName(), mockUser.getId()))
                .thenReturn(Optional.of(existingTag));

        Tag result = tagService.findOrCreateTag(existingTag.getName(), "#ffffff", mockUser);

        assertThat(result).isEqualTo(existingTag);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void findOrCreateTag_createsNew_whenNotFound() {
        Tag newTag = Tag.builder().id(2).name("Spring").hexColor("#6db33f").owner(mockUser).build();
        when(tagRepository.findByNameIgnoreCaseAndOwnerId("Spring", mockUser.getId()))
                .thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);

        Tag result = tagService.findOrCreateTag("Spring", "#6db33f", mockUser);

        assertThat(result.getName()).isEqualTo("Spring");
        verify(tagRepository).save(any(Tag.class));
    }
}
