package com.santana.chamados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.santana.chamados.security.JwtAuthFilter;

import jakarta.servlet.DispatcherType;

// regras de segurança da api inteira
@Configuration
@EnableWebSecurity
// liga o preauthorize nos controllers, é ele que barra quem não é tecnico ou admin
@EnableMethodSecurity
public class SecurityConfig {

        // monta a cadeia de filtros por onde toda requisição passa
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter)
                        throws Exception {
                http
                                // CSRF só serve para cookie e sessão, aqui o token vai no header então desliga
                                .csrf(csrf -> csrf.disable())
                                // sem sessão, cada requisição se autentica sozinha pelo JWT
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // sem token responde 401 vazio em vez do 403 padrão
                                .exceptionHandling(e -> e
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                // aqui define quem pode entrar em cada rota
                                .authorizeHttpRequests(auth -> auth
                                                // deixa passar o redirecionamento interno de erro para não esconder o erro real
                                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                                // rotas públicas: swagger, health e página de erro
                                                .requestMatchers(
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/actuator/health",
                                                                "/error")
                                                .permitAll()
                                                // o resto exige JWT válido
                                                .anyRequest().authenticated())
                                // o filtro do JWT roda antes do login padrão para o usuario já estar no contexto
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
