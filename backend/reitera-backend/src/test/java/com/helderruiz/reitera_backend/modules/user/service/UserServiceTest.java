package com.helderruiz.reitera_backend.modules.user.service;

import com.helderruiz.reitera_backend.core.email.EmailVerificationService;
import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.auth.service.RefreshTokenService;
import com.helderruiz.reitera_backend.modules.user.dto.AuthResponseDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserLoginDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserRegisterDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.repository.RoleRepository;
import com.helderruiz.reitera_backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private Role studentRole;
    private User savedUser;
    private UserRegisterDTO validRegisterDto;
    private UserLoginDTO validLoginDto;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        studentRole = Role.builder().id(1).name("STUDENT").build();
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("reitera")
                .email("reitera@test.com")
                .password("encoded-password")
                .firstName("Reitera")
                .lastName("Testez")
                .role(studentRole)
                .createdAt(LocalDateTime.now())
                .emailVerified(true)
                .build();
        validRegisterDto = new UserRegisterDTO(
                "reitera",
                "reitera@test.com",
                "Test@1234",
                "Test@1234",
                "Reitera",
                "Testez");
        validLoginDto = new UserLoginDTO("reitera@test.com", "Test@1234");
    }

    @Test
    void registerUser_success_returnsUserResponseDTO() {
        when(userRepository.findByEmail(validRegisterDto.email())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(validRegisterDto.username())).thenReturn(Optional.empty());
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doNothing().when(emailVerificationService).sendToken(any());

        UserResponseDTO result = userService.registerUser(validRegisterDto);

        assertThat(result.email()).isEqualTo(savedUser.getUsername());
        assertThat(result.username()).isEqualTo(savedUser.getNickname());
        assertThat(result.roleName()).isEqualTo("STUDENT");
    }

    @Test
    void registerUser_throws_whenPasswordsMismatch() {
        UserRegisterDTO dto = new UserRegisterDTO(
                validRegisterDto.username(),
                validRegisterDto.email(),
                "Test@1234",
                "Different@5678",
                validRegisterDto.firstName(),
                validRegisterDto.lastName());

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    void registerUser_throws_whenEmailTaken() {
        when(userRepository.findByEmail(validRegisterDto.email())).thenReturn(Optional.of(savedUser));

        assertThatThrownBy(() -> userService.registerUser(validRegisterDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The provided data is invalid or already in use");
    }

    @Test
    void registerUser_throws_whenUsernameTaken() {
        when(userRepository.findByEmail(validRegisterDto.email())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(validRegisterDto.username())).thenReturn(Optional.of(savedUser));

        assertThatThrownBy(() -> userService.registerUser(validRegisterDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The provided data is invalid or already in use");
    }

    @Test
    void getMe_returnsMappedDTO() {
        UserResponseDTO result = userService.getMe(savedUser);

        assertThat(result.id()).isEqualTo(savedUser.getId());
        assertThat(result.email()).isEqualTo(savedUser.getUsername());
        assertThat(result.roleName()).isEqualTo("STUDENT");
    }

    @Test
    void loginUser_success_returnsAuthResponse() {
        when(userRepository.findByEmail(validLoginDto.email())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(validLoginDto.password(), savedUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn("refresh-token");

        AuthResponseDTO result = userService.loginUser(validLoginDto);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void loginUser_throws_whenUserNotFound() {
        UserLoginDTO dto = new UserLoginDTO("unknown@test.com", validLoginDto.password());
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loginUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void loginUser_throws_whenPasswordWrong() {
        UserLoginDTO dto = new UserLoginDTO(validLoginDto.email(), "wrong-password");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(dto.password(), savedUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.loginUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
