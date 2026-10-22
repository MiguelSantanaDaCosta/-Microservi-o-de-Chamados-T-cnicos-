package com.santana.chamados.security;

import org.springframework.security.core.Authentication;

// checagem de papéis num lugar só para não repetir nos services
// só métodos estáticos, não precisa instanciar
public final class PapelUtil {
    private PapelUtil() {

    }

    // true se o usuario tem o papel de tecnico
    public static boolean isTecnico(Authentication auth) {
        return temPapel(auth, "ROLE_TECNICO");

    }

    // tecnico e admin obervam chamados de todos os usuarios
    public static boolean isPrivilegiado(Authentication auth) {
        return temPapel(auth, "ROLE_TECNICO") || temPapel(auth, "ROLE_ADMIN");

    }

    // procura o papel pedido entre as authorities do usuario
    private static boolean temPapel(Authentication auth, String papel) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(papel));

    }

}
