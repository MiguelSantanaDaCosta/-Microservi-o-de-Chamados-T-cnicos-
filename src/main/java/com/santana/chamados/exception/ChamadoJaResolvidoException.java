package com.santana.chamados.exception;

public class ChamadoJaResolvidoException extends RuntimeException {
    public ChamadoJaResolvidoException(String id) {
        super("Chamado " + id + " já está resolvido");
    }
}
