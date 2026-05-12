package com.helderruiz.reitera_backend.modules.card.controller;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
public class CardControllerTest {

    private User mockUser;
    private CardResponseDTO cardResponse;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setup() {
        Role mockRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder().id(UUID.randomUUID()).email("alumno@reitera.com").role(mockRole).build();
        cardResponse = new CardResponseDTO(
                1,
                1,
                "BASIC",
                "What is Java?",
                Map.of("answer", "A language"),
                null,
                Set.of(),
                null);
    }

    @Test
    void createCard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/decks/1/cards").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BASIC\",\"question\":\"What is Java?\",\"answerJson\":{\"answer\":\"A language\"}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCard_authenticatedUser_returns201() throws Exception {
        when(cardService.createCard(any(), any(), any())).thenReturn(cardResponse);

        mockMvc.perform(post("/api/v1/decks/1/cards").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BASIC\",\"question\":\"What is Java?\",\"answerJson\":{\"answer\":\"A language\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.question").value("What is Java?"));
    }

    @Test
    void createCard_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/decks/1/cards").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerJson\":{\"answer\":\"A language\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCardsByDeck_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/decks/1/cards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCardsByDeck_authenticatedUser_returns200WithPage() throws Exception {
        when(cardService.getCardsByDeck(any(), any(), any())).thenReturn(new PageImpl<>(List.of(cardResponse)));

        mockMvc.perform(get("/api/v1/decks/1/cards").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCardById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/decks/1/cards/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCardById_authenticatedUser_returns200() throws Exception {
        when(cardService.getCardById(any(), any(), any())).thenReturn(cardResponse);

        mockMvc.perform(get("/api/v1/decks/1/cards/1").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.question").value("What is Java?"));
    }

    @Test
    void getCardById_nonExistentCard_returns404() throws Exception {
        when(cardService.getCardById(any(), any(), any())).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(get("/api/v1/decks/1/cards/1").with(user(mockUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/decks/1/cards/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BASIC\",\"question\":\"Updated\",\"answerJson\":{\"answer\":\"A language\"}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCard_authenticatedOwner_returns200() throws Exception {
        when(cardService.updateCard(any(), any(), any(), any())).thenReturn(cardResponse);

        mockMvc.perform(put("/api/v1/decks/1/cards/1").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BASIC\",\"question\":\"Updated\",\"answerJson\":{\"answer\":\"A language\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateCard_nonExistentCard_returns404() throws Exception {
        when(cardService.updateCard(any(), any(), any(), any())).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(put("/api/v1/decks/1/cards/1").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BASIC\",\"question\":\"Updated\",\"answerJson\":{\"answer\":\"A language\"}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/decks/1/cards/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCard_authenticatedOwner_returns204() throws Exception {
        doNothing().when(cardService).deleteCard(any(), any(), any());

        mockMvc.perform(delete("/api/v1/decks/1/cards/1").with(user(mockUser)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCard_nonExistentCard_returns404() throws Exception {
        doThrow(ResourceNotFoundException.class).when(cardService).deleteCard(any(), any(), any());

        mockMvc.perform(delete("/api/v1/decks/1/cards/1").with(user(mockUser)).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
