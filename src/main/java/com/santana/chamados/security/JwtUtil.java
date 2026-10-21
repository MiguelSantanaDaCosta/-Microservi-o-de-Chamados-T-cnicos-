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

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public String extrairUsername(String token) {
        return extrairClaims(token).getSubject();
    }

    public boolean isPreAuth(String token) {
        return Boolean.TRUE.equals(extrairClaims(token).get("preAuth", Boolean.class));
    }

    public List<String> extrairRoles(String token) {
        Object roles = extrairClaims(token).get("roles");
        if (roles instanceof List<?> lista) {
            return lista.stream()
                    .map(Object::toString)
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .toList();
        }
        return List.of();
    }
}
