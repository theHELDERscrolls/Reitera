package com.helderruiz.reitera_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 * <p>
 * Declares the API metadata shown in the Swagger UI header and registers a
 * global Bearer JWT security scheme, enabling the "Authorize" button so that
 * a token entered once is automatically attached to every protected request.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Reitera API",
                version = "v1",
                description = "REST API for the Reitera spaced-repetition flashcard application."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
