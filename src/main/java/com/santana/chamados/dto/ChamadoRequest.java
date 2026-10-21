package com.santana.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
		@NotBlank(message = "O título é obrigatório") @Size(max = 120, message = "O título deve ter no máximo 120 caracteres") String titulo,

		@NotBlank(message = "A descrição é obrigatória") @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres") String descricao) {
}
