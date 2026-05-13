package com.helderruiz.reitera_backend.modules.dashboard.controller;

import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.dashboard.dto.DailyStudyCountDTO;
import com.helderruiz.reitera_backend.modules.dashboard.dto.DashboardStatsDTO;
import com.helderruiz.reitera_backend.modules.dashboard.dto.LastStudiedDeckDTO;
import com.helderruiz.reitera_backend.modules.dashboard.service.DashboardService;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {
    private User mockUser;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder().id(UUID.randomUUID()).email("reitera@test.com").role(studentRole).build();
    }

    @Test
    void getStats_returns200_whenAuthenticated() throws Exception {
        when(dashboardService.getStats(any())).thenReturn(new DashboardStatsDTO(3, 10L, 5L));

        mockMvc.perform(get("/api/v1/dashboard/stats").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").value(3))
                .andExpect(jsonPath("$.totalDueToday").value(10))
                .andExpect(jsonPath("$.studiedToday").value(5));
    }

    @Test
    void getLastStudied_returns200_withDefaultLimit() throws Exception {
        LastStudiedDeckDTO dto = new LastStudiedDeckDTO(
                1,
                "Java Basics",
                "CS",
                2L,
                3L,
                0L,
                "2026-05-13T10:00"
        );
        when(dashboardService.getLastStudied(any(), anyInt())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/dashboard/last-studied").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deckId").value(1))
                .andExpect(jsonPath("$[0].name").value("Java Basics"));
    }

    @Test
    void getHeatmap_returns200() throws Exception {
        when(dashboardService.getHeatmap(any()))
                .thenReturn(List.of(new DailyStudyCountDTO("2026-05-13", 7L)));

        mockMvc.perform(get("/api/v1/dashboard/heatmap").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-05-13"))
                .andExpect(jsonPath("$[0].count").value(7));
    }

    @Test
    void allEndpoints_return401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/last-studied")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/heatmap")).andExpect(status().isUnauthorized());
    }
}
