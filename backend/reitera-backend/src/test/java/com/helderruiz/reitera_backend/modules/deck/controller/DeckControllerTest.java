package com.helderruiz.reitera_backend.modules.deck.controller;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckRequestDTO;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.dto.DeckStatsDTO;
import com.helderruiz.reitera_backend.modules.deck.service.DeckService;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(DeckController.class)
class DeckControllerTest {
    private User mockUser;
    private DeckResponseDTO deckResponse;
    private DeckRequestDTO deckRequest;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeckService deckService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Role mockRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder().id(UUID.randomUUID()).email("alumno@reitera.com").role(mockRole).build();
        deckResponse = new DeckResponseDTO(
                1,
                "My test deck",
                "Description",
                false,
                "Reitera Test",
                null,
                null,
                null,
                null,
                0L,
                0L,
                0L
        );

        deckRequest = new DeckRequestDTO(
                "New deck test",
                "New deck description",
                false,
                1,
                "Test Category"
        );
    }

    @Test
    void getDeckById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/decks/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void getDeckById_authenticatedUser_returns200() throws Exception {
        when(deckService.getDeckById(any(), any())).thenReturn(deckResponse);

        mockMvc.perform(get("/api/v1/decks/1").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("My test deck"))
                .andExpect(jsonPath("$.description").value("Description"));
    }

    @Test
    void getDeckById_nonExistentDeck_returns404() throws Exception {
        when(deckService.getDeckById(any(), any())).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(get("/api/v1/decks/1").with(user(mockUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDeck_authenticatedUser_returns201WithBody() throws Exception {
        when(deckService.createDeck(any(), any())).thenReturn(deckResponse);

        mockMvc.perform(post("/api/v1/decks").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"New deck test\", \"description\": \"New deck description\", \"isPublic\": false, \"categoryName\": \"Test Category\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createDeck_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/decks").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"New deck\", \"isPublic\": false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDeck_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/decks").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"No title here\", \"isPublic\": false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserDecks_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/decks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserDecks_authenticatedUser_returns200WithPage() throws Exception {
        when(deckService.getUserDecks(any(), any(), any())).thenReturn(new PageImpl<>(List.of(deckResponse)));

        mockMvc.perform(get("/api/v1/decks").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateDeck_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/decks/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Updated\", \"isPublic\": false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateDeck_authenticatedOwner_returns200() throws Exception {
        when(deckService.updateDeck(any(), any(), any())).thenReturn(deckResponse);

        mockMvc.perform(put("/api/v1/decks/1").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Updated\", \"isPublic\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateDeck_nonExistentDeck_returns404() throws Exception {
        when(deckService.updateDeck(any(), any(), any())).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(put("/api/v1/decks/1").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Updated\", \"isPublic\": false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDeck_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/decks/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteDeck_authenticatedOwner_returns204() throws Exception {
        doNothing().when(deckService).deleteDeck(any(), any());

        mockMvc.perform(delete("/api/v1/decks/1").with(user(mockUser)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDeck_nonExistentDeck_returns404() throws Exception {
        doThrow(ResourceNotFoundException.class).when(deckService).deleteDeck(any(), any());

        mockMvc.perform(delete("/api/v1/decks/1").with(user(mockUser)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDeckStats_authenticatedUser_returns200() throws Exception {
        DeckStatsDTO stats = new DeckStatsDTO(10L, 3L, 2L, 4L, 1L, 5L);
        when(deckService.getDeckStats(any(), any())).thenReturn(stats);

        mockMvc.perform(get("/api/v1/decks/1/stats").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(10))
                .andExpect(jsonPath("$.newCards").value(3));
    }

    @Test
    void getDeckStats_nonExistentDeck_returns404() throws Exception {
        when(deckService.getDeckStats(any(), any())).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(get("/api/v1/decks/1/stats").with(user(mockUser)))
                .andExpect(status().isNotFound());
    }
}
