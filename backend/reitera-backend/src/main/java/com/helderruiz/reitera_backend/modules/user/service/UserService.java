package com.helderruiz.reitera_backend.modules.user.service;

import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import com.helderruiz.reitera_backend.modules.user.dto.AuthResponseDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserLoginDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserRegisterDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.model.Role;
import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.repository.RoleRepository;
import com.helderruiz.reitera_backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Core business logic for user management and authentication processes.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user in the system.
     * Validates uniqueness, assigns a default role, and hashes the password.
     */
    public UserResponseDTO registerUser(UserRegisterDTO dto) {
        // Verify uniqueness of email and username
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new RuntimeException("The email is already registered");
        }

        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new RuntimeException("The username is already in use");
        }

        // Retrieve the default 'STUDENT' role
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new RuntimeException("Error: STUDENT role not found in database"));

        // Map DTO to User entity and encode the plaintext password
        User newUser = User.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .role(studentRole)
                .build();

        // Persist the new user
        User savedUser = userRepository.save(newUser);

        // Return safe DTO without sensitive data
        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole().getName()
        );
    }

    /**
     * Returns the profile of the currently authenticated user.
     */
    public UserResponseDTO getMe(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName()
        );
    }

    /**
     * Authenticates a user and generates a JWT.
     * Prevents user enumeration by throwing generic exceptions on failure.
     */
    public AuthResponseDTO loginUser(UserLoginDTO dto) {
        // Fetch user by email. Throws generic exception to avoid credential enumeration.
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Verify the plaintext password against the stored BCrypt hash
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate the stateless JWT for the authenticated user
        String token = jwtService.generateToken(user.getUsername(), user.getRole().getName());

        return new AuthResponseDTO(token, "Login successful");
    }
}