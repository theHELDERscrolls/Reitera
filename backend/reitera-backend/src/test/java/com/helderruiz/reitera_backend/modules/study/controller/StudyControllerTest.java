package com.helderruiz.reitera_backend.modules.study.controller;

import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.study.dto.DueCardDTO;
import com.helderruiz.reitera_backend.modules.study.dto.StudySessionResponseDTO;
import com.helderruiz.reitera_backend.modules.study.service.StudyService;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyController.class)
class StudyControllerTest {
    private User mockUser;
    private DueCardDTO dueCard;
    private StudySessionResponseDTO sessionResponse;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyService studyService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder().id(UUID.randomUUID()).email("reitera@test.com").role(studentRole).build();
        dueCard = new DueCardDTO(
                1,
                1,
                "BASIC",
                "What is 2+2?",
                null,
                null,
                0);
        sessionResponse = new StudySessionResponseDTO(1, null, 1);
    }

    @Test
    void getDueCards_byDeckId_returns200() throws Exception {
        when(studyService.getDueCards(eq(1), isNull(), any())).thenReturn(List.of(dueCard));

        mockMvc.perform(get("/api/v1/study/due").param("deckId", "1").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].deckId").value(1));
    }

    @Test
    void getDueCards_byCategoryId_returns200() throws Exception {
        when(studyService.getDueCards(isNull(), eq(1), any())).thenReturn(List.of(dueCard));

        mockMvc.perform(get("/api/v1/study/due").param("categoryId", "1").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getDueCards_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/study/due").param("deckId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void processSession_returns201_whenValid() throws Exception {
        when(studyService.processSession(any(), any())).thenReturn(sessionResponse);

        mockMvc.perform(post("/api/v1/study/sessions").with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":1,\"ratings\":[{\"cardId\":1,\"rating\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deckId").value(1))
                .andExpect(jsonPath("$.cardsReviewed").value(1));
    }

    @Test
    void processSession_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/study/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":1,\"ratings\":[{\"cardId\":1,\"rating\":3}]}"))
                .andExpect(status().isUnauthorized());
    }
}
