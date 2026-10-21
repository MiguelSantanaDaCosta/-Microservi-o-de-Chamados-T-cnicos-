package com.santana.chamados.security;

import org.springframework.security.core.Authentication;

public final class PapelUtil {
    private PapelUtil() {

    }

    public static boolean isTecnico(Authentication auth) {
        return temPapel(auth, "ROLE_TECNICO");

    }

    // tecnico e admin obervam chamados de todos os usuarios
    public static boolean isPrivilegiado(Authentication auth) {
        return temPapel(auth, "ROLE_TECNICO") || temPapel(auth, "ROLE_ADMIN");

    }

    private static boolean temPapel(Authentication auth, String papel) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(papel));

    }

}
