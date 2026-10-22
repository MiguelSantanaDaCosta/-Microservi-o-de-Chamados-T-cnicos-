package com.santana.chamados.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import com.santana.chamados.entity.Chamado;

public interface ChamadoRepository extends MongoRepository<Chamado, String> {
	List<Chamado> findBySolicitanteOrderByDataAberturaDesc(String solicitante);
}
