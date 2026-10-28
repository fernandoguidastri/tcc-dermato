package com.tcc.dermato.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao da documentacao Swagger/OpenAPI.
 * Acesse em: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI dermatoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Plataforma Inteligente para Educação Dermatológica")
                        .version("1.0.0")
                        .description("""
                                API REST do sistema de apoio à análise dermatológica com IA (conceito Human-in-the-loop).

                                **Fluxo principal:** cadastrar paciente → registrar consulta → enviar imagem da lesão \
                                (o backend consulta o microsserviço de IA) → médico valida o diagnóstico.

                                **Como testar endpoints protegidos:** execute `POST /api/auth/login`, copie o `token` \
                                retornado, clique em **Authorize** (cadeado, no topo) e cole o token.""")
                        .contact(new Contact().name("TCC 2026 - Fernando Guidastri")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
