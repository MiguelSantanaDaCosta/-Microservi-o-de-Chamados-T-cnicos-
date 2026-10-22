package com.santana.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// corpo do PUT de resolver chamado
// só a solução vem do cliente, tecnico, status e data o servidor define
// não pode vir vazia nem passar de 2000 caracteres
public record ResolucaoRequest(
		@NotBlank(message = "A solução é obrigatoria") @Size(max = 2000, message = "A solução deve ter no maximo 2000 caracteres") String solucao) {
}
