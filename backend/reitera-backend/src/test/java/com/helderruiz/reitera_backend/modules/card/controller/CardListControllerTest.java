package com.helderruiz.reitera_backend.modules.card.controller;

import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.card.dto.CardResponseDTO;
import com.helderruiz.reitera_backend.modules.card.service.CardService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardListController.class)
class CardListControllerTest {
    private User mockUser;
    private CardResponseDTO card;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder().id(UUID.randomUUID()).email("reitera@test.com").role(studentRole).build();
        card = new CardResponseDTO(
                1,
                1,
                "BASIC",
                "What is JVM?",
                null,
                null,
                0);
    }

    @Test
    void getCards_returns200_withDefaultPagination() throws Exception {
        when(cardService.getAllCards(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(card)));

        mockMvc.perform(get("/api/v1/cards").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCards_returns200_withTypeFilter() throws Exception {
        when(cardService.getAllCards(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(card)));

        mockMvc.perform(get("/api/v1/cards").param("type", "BASIC").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("BASIC"));
    }

    @Test
    void getCards_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isUnauthorized());
    }
}
