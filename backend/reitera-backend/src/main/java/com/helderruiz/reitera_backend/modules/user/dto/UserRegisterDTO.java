package com.helderruiz.reitera_backend.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterDTO(
        @NotBlank(message = "The username is mandatory")
        @Size(min = 3, max = 50, message = "The username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "The email is mandatory")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email,

        @NotBlank(message = "The password is mandatory")
        @Size(max = 128, message = "Password cannot exceed 128 characters")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
                message = "The password must be at least 8 characters long and contain at least one digit, one lowercase letter, one uppercase letter, and one special character (@#$%^&+=!)"
        )
        String password,

        @NotBlank(message = "The confirm password is mandatory")
        @Size(max = 128, message = "Password cannot exceed 128 characters")
        String confirmPassword,

        @NotBlank(message = "The first name is mandatory")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,

        @NotBlank(message = "The last name is mandatory")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName
) {
}
