package com.santana.chamados.security;

import org.springframework.security.core.Authentication;

// checagem de papéis num lugar só para não repetir nos services
// só métodos estáticos, não precisa instanciar
//
// Hierarquia (a mesma do Thindesk, ver entity/Role.java): ROOT > ADMIN > TECNICO > USUARIO
//   USUARIO  -> abre chamados e vê os próprios
//   TECNICO  -> + vê os de qualquer usuário, vê a fila de abertos e resolve
//   ADMIN    -> tudo do técnico (a gestão de usuários é do Thindesk)
//   ROOT     -> tudo
// O Thindesk já envia no JWT os papéis expandidos; aqui a hierarquia é reaplicada para que um token só com
// ROLE_ADMIN ou ROLE_ROOT também funcione (defesa em profundidade, sem depender de quem emitiu o token).
public final class PapelUtil {
    private PapelUtil() {

    }

    // true se o usuario tem poderes de tecnico (tecnico, admin ou root)
    public static boolean isTecnico(Authentication auth) {
        return isPrivilegiado(auth);
    }

    // tecnico, admin e root observam chamados de todos os usuarios e resolvem
    public static boolean isPrivilegiado(Authentication auth) {
        return temPapel(auth, "ROLE_TECNICO") || temPapel(auth, "ROLE_ADMIN") || temPapel(auth, "ROLE_ROOT");

    }

    // procura o papel pedido entre as authorities do usuario
    private static boolean temPapel(Authentication auth, String papel) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(papel));

    }

}
