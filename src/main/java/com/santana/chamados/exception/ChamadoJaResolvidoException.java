package com.santana.chamados.exception;

// lançada quando tentam alterar um chamado que já está resolvido
// o handler global transforma ela em 409
public class ChamadoJaResolvidoException extends RuntimeException {
    public ChamadoJaResolvidoException(String id) {
        super("Chamado " + id + " já está resolvido");
    }
}
