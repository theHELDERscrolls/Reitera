package com.helderruiz.reitera_backend.modules.user.controller;

import com.helderruiz.reitera_backend.core.exception.DataConflictException;
import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {
    private User mockUser;
    private UserResponseDTO userResponse;

    private static final String VALID_UPDATE_BODY =
            "{\"username\":\"reitera\",\"firstName\":\"Reitera\",\"lastName\":\"Testez\",\"avatarId\":null}";

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
                "Reitera", "Testez", null, LocalDateTime.now());
    }

    @Test
    void getMe_returns200_whenAuthenticated() throws Exception {
        when(userService.getMe(any())).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/me").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("reitera@test.com"))
                .andExpect(jsonPath("$.username").value("reitera"));
    }

    @Test
    void getMe_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMe_returns200_whenAuthenticated() throws Exception {
        UserResponseDTO updated = new UserResponseDTO(
                mockUser.getId(), "newname", "reitera@test.com",
                "New", "Name", "avatar-01", LocalDateTime.now());
        when(userService.updateMe(any(), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newname\",\"firstName\":\"New\",\"lastName\":\"Name\",\"avatarId\":\"avatar-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newname"))
                .andExpect(jsonPath("$.avatarId").value("avatar-01"));
    }

    @Test
    void updateMe_returns409_whenUsernameTaken() throws Exception {
        when(userService.updateMe(any(), any()))
                .thenThrow(new DataConflictException("The provided data is invalid or already in use"));

        mockMvc.perform(put("/api/v1/users/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("The provided data is invalid or already in use"));
    }

    @Test
    void updateMe_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isUnauthorized());
    }
}
