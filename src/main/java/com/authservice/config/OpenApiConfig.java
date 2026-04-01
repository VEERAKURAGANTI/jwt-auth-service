package com.authservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "JWT Auth Service API",
        version     = "1.0.0",
        description = "Spring Boot JWT Authentication Service. " +
                      "Register users, login, refresh tokens, " +
                      "and manage roles with full RBAC support.",
        contact = @Contact(
            name  = "Auth Service",
            email = "admin@authservice.com"
        )
    )
)
@SecurityScheme(
    // This name is referenced in controllers via:
    // @SecurityRequirement(name = "Bearer Authentication")
    name        = "Bearer Authentication",

    // HTTP authentication type (as opposed to API key, OAuth2 etc.)
    type        = SecuritySchemeType.HTTP,

    // "bearer" tells Swagger this is a Bearer token scheme
    scheme      = "bearer",

    // Documents that the token format is JWT (informational only)
    bearerFormat = "JWT",

    description = "Paste your JWT access token here. " +
                  "Get one from POST /api/auth/login. " +
                  "Format: just the token (no 'Bearer' prefix needed here)"
)
public class OpenApiConfig {

}
