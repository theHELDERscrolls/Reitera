package com.helderruiz.reitera_backend.modules.deck.service;

import com.helderruiz.reitera_backend.modules.deck.dto.CategoryResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.model.Category;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    private User mockUser;
    private Category category;

    @Mock
    private DeckRepository deckRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(UUID.randomUUID()).build();
        category = Category.builder()
                .id(1)
                .name("Computer Science")
                .description("CS fundamentals")
                .build();
    }

    @Test
    void getMyCategories_returnsMappedDTOs() {
        when(deckRepository.findDistinctCategoriesByOwnerId(mockUser.getId()))
                .thenReturn(List.of(category));

        List<CategoryResponseDTO> result = categoryService.getMyCategories(mockUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(category.getId());
        assertThat(result.get(0).name()).isEqualTo(category.getName());
        assertThat(result.get(0).description()).isEqualTo(category.getDescription());
    }
}
