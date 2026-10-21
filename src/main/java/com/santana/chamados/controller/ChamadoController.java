package com.santana.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.santana.chamados.dto.ChamadoRequest;
import com.santana.chamados.entity.Chamado;
import com.santana.chamados.service.ChamadoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chamados")
@RequiredArgsConstructor
public class ChamadoController {

	private final ChamadoService service;

	@PostMapping
	public ResponseEntity<Chamado> abrir(@Valid @RequestBody ChamadoRequest request,
			Authentication auth) {
		Chamado criado = service.criar(request, auth.getName());
		return ResponseEntity
				.created(URI.create("/api/chamados/" + criado.getId()))
				.body(criado);
	}

	@GetMapping("/usuario/{id}")
	public ResponseEntity<List<Chamado>> listarPorUsuario(@PathVariable("id") String usuario,
			Authentication auth) {
		return ResponseEntity.ok(service.listarPorUsuario(usuario, auth));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Chamado> buscar(@PathVariable String id, Authentication auth) {
		return ResponseEntity.ok(service.buscar(id, auth));
	}
}
