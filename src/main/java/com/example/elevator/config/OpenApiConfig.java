package com.example.elevator.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI metadata, including a reusable "bearerAuth" JWT scheme
 * so /swagger-ui.html shows an Authorize button for testing secured
 * endpoints directly from the browser.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Elevator Management System API", version = "1.0",
                description = "RESTful API for multi-elevator coordination, scheduling and fault tolerance")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
