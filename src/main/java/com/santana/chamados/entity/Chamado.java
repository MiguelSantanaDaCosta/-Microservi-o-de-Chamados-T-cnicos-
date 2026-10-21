package com.santana.chamados.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.santana.chamados.enums.Status;

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

}
