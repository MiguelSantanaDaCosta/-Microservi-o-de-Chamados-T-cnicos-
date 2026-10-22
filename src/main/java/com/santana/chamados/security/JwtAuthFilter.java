package com.santana.chamados.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// filtro que roda uma vez por requisição e autentica pelo JWT do header authorization
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        // o token vem no header authorization no formato bearer mais o token
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            // tira o bearer e o espaço, sobra só o token
            String token = header.substring(7);
            // só autentica se o token for válido e não for o token parcial do login (preauth)
            if (jwtUtil.isTokenValido(token) && !jwtUtil.isPreAuth(token)) {
                // o username é o sub do token
                String username = jwtUtil.extrairUsername(token);
                // transforma cada papel do token numa authority do spring security
                List<GrantedAuthority> authorities = jwtUtil.extrairRoles(token).stream()
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r))
                        .toList();
                // sem senha aqui, o token já foi validado
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                        authorities);
                // guarda o usuario no contexto, daqui para frente a requisição conta como autenticada
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        // sem token ou com token ruim segue sem autenticação e o spring responde 401 nas rotas protegidas
        filterChain.doFilter(request, response);
    }
}
