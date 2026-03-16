package com.helderruiz.reitera_backend.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginDTO(
        @NotBlank(message = "The email is mandatory")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "The password is mandatory")
        String password
) {
}