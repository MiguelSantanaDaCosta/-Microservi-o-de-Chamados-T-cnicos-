package com.santana.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// corpo do POST de abrir chamado
// o cliente manda só título e descrição, o resto o servidor define
// não pode vir vazio nem passar do limite, senão volta 400
public record ChamadoRequest(
		@NotBlank(message = "O título é obrigatório") @Size(max = 120, message = "O título deve ter no máximo 120 caracteres") String titulo,

		@NotBlank(message = "A descrição é obrigatória") @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres") String descricao) {
}
