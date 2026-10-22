package com.santana.chamados.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import com.santana.chamados.entity.Chamado;

// acesso à coleção chamados no mongo
// salvar, buscar por id e apagar já vêm prontos do spring data
@JaversSpringDataAuditable
public interface ChamadoRepository extends MongoRepository<Chamado, String> {
	// o spring monta a query só pelo nome do método
	// busca por solicitante e ordena pela data de abertura, do mais recente para o mais antigo
	List<Chamado> findBySolicitanteOrderByDataAberturaDesc(String solicitante);
}
