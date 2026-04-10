package com.sensedia.consentapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI/Swagger com suporte a autenticação JWT.
 *
 * CONCEITO — Por que precisamos desta configuração?
 * =================================================
 * Por padrão, o Swagger UI não sabe que nossa API usa JWT.
 * Sem esta configuração, não aparece o botão "Authorize" e
 * não há como enviar o token Bearer nos headers das requisições.
 *
 * Esta classe registra um SecurityScheme do tipo "bearer" com formato "JWT",
 * e aplica esse esquema globalmente a todos os endpoints.
 * Assim, ao clicar em "Authorize" no Swagger UI, você pode colar o token
 * e todas as requisições subsequentes incluirão o header:
 *   Authorization: Bearer <seu_token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Consent API - Open Insurance")
                        .version("1.0")
                        .description("API para gestão de consentimentos de usuários, "
                                + "com autenticação JWT e controle de acesso baseado em roles."))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT obtido no endpoint /auth/login. "
                                                + "Exemplo: eyJhbGciOiJIUzI1NiJ9...")));
    }
}
