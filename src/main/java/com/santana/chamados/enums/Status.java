package com.santana.chamados.enums;
// ciclo de vida do chamado
// nasce aberto e depois de resolvido não muda mais
public enum Status {
        // estado inicial, o servidor define na criação
        ABERTO,
        // estado final, depois dele não dá para alterar
        RESOLVIDO
    }
