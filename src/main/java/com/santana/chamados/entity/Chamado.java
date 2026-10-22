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

// documento da coleção chamados no mongo
// o lombok gera getters, setters e o builder
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chamados")
public class Chamado {

	// id gerado pelo mongo
	@Id
	private String id;
	private String titulo;
	private String descricao;

	// solicitante tem índice porque a listagem por usuario filtra por ele
	@Indexed
	private String solicitante;
	// aberto ou resolvido, quem define é o servidor
	private Status status;
	// data e hora em UTC do momento da abertura
	private Instant dataAbertura;
	// solução, tecnico e data de resolução só são preenchidos quando o chamado é resolvido
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
