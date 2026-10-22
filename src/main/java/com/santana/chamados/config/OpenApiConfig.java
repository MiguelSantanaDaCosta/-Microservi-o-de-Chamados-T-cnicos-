package com.santana.chamados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

// configuração do swagger
@Configuration
public class OpenApiConfig {
    
    // nome do esquema de segurança, o controller usa o mesmo nome para ligar o cadeado
    public static final String ESQUEMA_JWT = "bearerAuth";

    // monta a documentação e cria o botão authorize para colar o token JWT
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // título e descrição que aparecem no topo do swagger
                .info(new Info()
                        .title("ms-chamados")
                        .description("Microserviço de chamados técnicos do Thindesk"))
                // avisa o swagger que a autenticação é bearer com JWT
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
