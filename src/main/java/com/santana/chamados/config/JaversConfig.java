package com.santana.chamados.config;

import org.javers.spring.auditable.AuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// configuração da auditoria do javers
@Configuration
public class JaversConfig {

    // o javers chama isso a cada save para saber quem mexeu no chamado
    @Bean
    public AuthorProvider authorProvider() {
        return () -> {
            // pega o usuario logado que o filtro do JWT colocou no contexto de segurança
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // sem ninguém logado grava como anonimo
            return auth != null ? auth.getName() : "anonimo";
        };
    }
}
