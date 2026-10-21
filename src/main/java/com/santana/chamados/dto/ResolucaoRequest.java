package com.santana.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolucaoRequest(
		@NotBlank(message = "A solução é obrigatoria") @Size(max = 2000, message = "A solução deve ter no maximo 2000 caracteres") String solucao) {
}
