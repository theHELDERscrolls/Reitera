package com.helderruiz.reitera_backend.modules.note.controller;

import com.helderruiz.reitera_backend.core.exception.ResourceNotFoundException;
import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.note.dto.NoteResponseDTO;
import com.helderruiz.reitera_backend.modules.note.service.NoteService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(NoteController.class)
class NoteControllerTest {

    private static final String BASE_URL = "/api/v1/decks/1/notes";
    private static final String VALID_BODY = """
            {"content": "What year?\\n\\n---\\n\\n1936", "explanation": null}
            """;

    private User mockUser;
    private NoteResponseDTO noteResponse;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService noteService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder().id(UUID.randomUUID()).email("alumno@reitera.com").role(studentRole).build();
        noteResponse = new NoteResponseDTO(1, 1, "BASIC", "What year?\n\n---\n\n1936", null, 1, null);
    }

    // POST

    @Test
    void createNote_returns201_withValidBody() throws Exception {
        when(noteService.createNote(any(), any(), any())).thenReturn(noteResponse);

        mockMvc.perform(post(BASE_URL).with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("BASIC"));
    }

    @Test
    void createNote_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createNote_returns400_whenContentIsBlank() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"\", \"explanation\": null}"))
                .andExpect(status().isBadRequest());
    }

    // GET list

    @Test
    void getNotesByDeck_returns200_withPaginatedList() throws Exception {
        when(noteService.getNotesByDeck(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(noteResponse)));

        mockMvc.perform(get(BASE_URL).with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // GET single

    @Test
    void getNoteById_returns200_whenFound() throws Exception {
        when(noteService.getNoteById(any(), eq(1), any())).thenReturn(noteResponse);

        mockMvc.perform(get(BASE_URL + "/1").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("BASIC"));
    }

    @Test
    void getNoteById_returns404_whenNotFound() throws Exception {
        when(noteService.getNoteById(any(), any(), any())).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(get(BASE_URL + "/99").with(user(mockUser)))
                .andExpect(status().isNotFound());
    }

    // PUT

    @Test
    void updateNote_returns200_withValidBody() throws Exception {
        when(noteService.updateNote(any(), any(), any(), any())).thenReturn(noteResponse);

        mockMvc.perform(put(BASE_URL + "/1").with(user(mockUser)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // DELETE

    @Test
    void deleteNote_returns204_whenFound() throws Exception {
        doNothing().when(noteService).deleteNote(any(), any(), any());

        mockMvc.perform(delete(BASE_URL + "/1").with(user(mockUser)).with(csrf()))
                .andExpect(status().isNoContent());
    }
}
