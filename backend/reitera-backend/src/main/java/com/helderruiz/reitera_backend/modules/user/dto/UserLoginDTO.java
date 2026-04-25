package com.helderruiz.reitera_backend.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginDTO(
        @NotBlank(message = "The email is mandatory")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email,

        @NotBlank(message = "The password is mandatory")
        @Size(max = 128, message = "Password cannot exceed 128 characters")
        String password
) {
}
