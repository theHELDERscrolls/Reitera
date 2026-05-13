package com.helderruiz.reitera_backend.modules.deck.controller;

import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.service.CardService;
import com.helderruiz.reitera_backend.modules.deck.dto.TagResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.service.TagService;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
class TagControllerTest {
    private User mockUser;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder().id(UUID.randomUUID()).email("reitera@test.com").role(studentRole).build();
    }

    @Test
    void getMyTags_returns200_whenAuthenticated() throws Exception {
        when(tagService.getMyTags(any()))
                .thenReturn(List.of(new TagResponseDTO(1, "Java", "#f89820")));

        mockMvc.perform(get("/api/v1/tags").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    void getMyTags_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCardsByTag_returns200_withPage() throws Exception {
        CardResponseDTO card = new CardResponseDTO(1, 1, "BASIC", "What is JVM?", null, null, null, 0);
        when(cardService.getCardsByTag(eq(1), any(), any()))
                .thenReturn(new PageImpl<>(List.of(card)));

        mockMvc.perform(get("/api/v1/tags/1/cards").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCardsByTag_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/tags/1/cards"))
                .andExpect(status().isUnauthorized());
    }
}
