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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponseDTO registerUser(UserRegisterDTO dto) {
        // 1. Validaciones básicas: Que no se repitan email ni username
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new RuntimeException("The email is already registered");
        }

        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new RuntimeException("The username is already in use");
        }

        // 2. Buscar el rol por defecto en la BD. Si no existe, lanza excepción.
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new RuntimeException("Error: STUDENT role not found in database"));

        // 3. Mapear el DTO (record) a la Entidad User y encriptar la password
        User newUser = User.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .role(studentRole)
                .build();

        // 4. Guardar en la base de datos
        User savedUser = userRepository.save(newUser);

        // 5. Devolver el DTO de respuesta (ocultando la contraseña y pasando solo el nombre del rol)
        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole().getName()
        );
    }

    public AuthResponseDTO loginUser(UserLoginDTO dto) {
        // 1. Buscar al usuario por email en la BD
        User user = userRepository.findByEmail(dto.email())
                // Por seguridad, si falla el email o la contraseña, devolvemos el mismo error genérico para no dar pistas a los hackers.
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // 2. Comprobar que la contraseña es correcta
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // 3. Generar el JWT
        String token = jwtService.generateToken(user.getUsername(), user.getRole().getName());

        // 4. Devolver el DTO con el token
        return new AuthResponseDTO(token, "Login successful");
    }
}