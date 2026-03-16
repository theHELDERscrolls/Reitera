package com.helderruiz.reitera_backend.modules.auth;

import com.helderruiz.reitera_backend.modules.user.dto.AuthResponseDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserLoginDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserRegisterDTO;
import com.helderruiz.reitera_backend.modules.user.dto.UserResponseDTO;
import com.helderruiz.reitera_backend.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // POST http://localhost:8080/api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegisterDTO dto) {
        // Le pasamos el DTO al servicio y nos devuelve el DTO de respuesta
        UserResponseDTO response = userService.registerUser(dto);

        // Devolvemos un HTTP 201 (Created) con el usuario recién creado en el cuerpo (body)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST http://localhost:8080/api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody UserLoginDTO dto) {
        // Le pasamos las credenciales al servicio
        AuthResponseDTO response = userService.loginUser(dto);

        // Devolvemos un HTTP 200 (OK) con el token en el cuerpo (body)
        return ResponseEntity.ok(response);
    }
}