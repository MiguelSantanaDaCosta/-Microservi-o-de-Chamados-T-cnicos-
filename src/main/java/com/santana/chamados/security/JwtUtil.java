package com.santana.chamados.security;

import java.util.List;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// valida o JWT que o thindesk emitiu e tira as informações dele
// aqui não gera token nem faz login
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // segredo que vem do .env, tem que ser igual ao do thindesk
    @Value("${app.jwt.secret}")
    private String secret;

    // monta a chave HS256 a partir do segredo
    // precisa de no mínimo 32 caracteres
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // confere a assinatura e a validade e devolve o conteúdo do token
    // se for falso ou estiver expirado lança exceção
    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // diz se o token é íntegro e não expirou
    // em vez de estourar exceção devolve false
    public boolean isTokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    // devolve o username (claim sub)
    public String extrairUsername(String token) {
        return extrairClaims(token).getSubject();
    }

    // token preauth é só a etapa do meio do login do thindesk
    // ele não autentica ninguém aqui
    public boolean isPreAuth(String token) {
        return Boolean.TRUE.equals(extrairClaims(token).get("preAuth", Boolean.class));
    }

    // lê a lista de papéis do token
    public List<String> extrairRoles(String token) {
        Object roles = extrairClaims(token).get("roles");
        if (roles instanceof List<?> lista) {
            return lista.stream()
                    .map(Object::toString)
                    // o spring exige o prefixo role_ nos papéis, se faltar a gente coloca
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .toList();
        }
        // sem papéis no token devolve lista vazia, ou seja usuario comum
        return List.of();
    }
}
