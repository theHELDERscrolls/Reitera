package com.helderruiz.reitera_backend.modules.user.controller;

import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {
    private User mockUser;
    private UserResponseDTO userResponse;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1).name("STUDENT").build();
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("reitera@test.com")
                .role(studentRole)
                .build();
        userResponse = new UserResponseDTO(
                mockUser.getId(), "reitera", "reitera@test.com",
                "Reitera", "Testez", "STUDENT", LocalDateTime.now());
    }

    @Test
    void getMe_returns200_whenAuthenticated() throws Exception {
        when(userService.getMe(any())).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/me").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("reitera@test.com"))
                .andExpect(jsonPath("$.username").value("reitera"))
                .andExpect(jsonPath("$.roleName").value("STUDENT"));
    }

    @Test
    void getMe_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
