package com.santana.chamados.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.santana.chamados.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chamados")
public class Chamado {

	@Id
	private String id;
	private String titulo;
	private String descricao;

	@Indexed
	private String solicitante;
	private Status status;
	private Instant dataAbertura;
	private String solucao;
	private String tecnico;
	private Instant dataResolucao;

	// chamado é criado aberto com data do servidor
	public static Chamado abrir(String titulo, String descricao, String solicitante) {
		return Chamado.builder()
				.titulo(titulo.trim())
				.descricao(descricao.trim())
				.solicitante(solicitante)
				.status(Status.ABERTO)
				.dataAbertura(Instant.now())
				.build();
	}

	// status tecnico e data são definidos pelo servidor
	public void registrarResolucao(String solucao, String tecnico) {
		this.solucao = solucao.trim();
		this.tecnico = tecnico;
		this.status = Status.RESOLVIDO;
		this.dataResolucao = Instant.now();
	}
}
