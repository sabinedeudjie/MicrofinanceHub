package org.example.accountservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de Swagger UI.
 *
 * Ajoute un bouton "Authorize" dans l'interface Swagger
 * pour entrer le token JWT une seule fois et l'envoyer
 * automatiquement sur tous les endpoints sécurisés.
 *
 * Accès : http:///swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MicrofinanceHub — Account Service")
                        .description("API de gestion des comptes et transactions. " +
                                "Cliquez sur 'Authorize' et entrez votre token JWT pour tester les endpoints sécurisés.")
                        .version("1.0.0"))
                //  le schéma de sécurité Bearer JWT
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Entrez votre token JWT (sans le préfixe 'Bearer')")));
    }
}
