package com.thejas.ai_frms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI aiFrmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Based FRMS Rule Testing Assistant API")
                        .description("Backend APIs for managing FRMS rules, transactions, test cases, executions, AI test generation, reports, and dashboard.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Thejas HG")
                                .email("thejashg88@gmail.com"))
                        .license(new License()
                                .name("Internal Project")
                                .url("https://example.com")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token from /api/auth/login response")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}