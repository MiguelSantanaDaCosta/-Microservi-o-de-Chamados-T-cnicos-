// ============================================================
// ChamadoController.java
// Autor: Miguel Santana
// Descrição: Endpoints REST de chamados (JSON). Autenticação via
//            JWT do ThinDesk.
// ============================================================
package com.santana.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.santana.chamados.config.OpenApiConfig;
import com.santana.chamados.dto.ChamadoRequest;
import com.santana.chamados.dto.ResolucaoRequest;
import com.santana.chamados.entity.Chamado;
import com.santana.chamados.service.ChamadoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chamados")
@RequiredArgsConstructor
@Tag(name = "Chamados", description = "Abertura, acompanhamento e resolução de chamados técnicos")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class ChamadoController {

    private final ChamadoService service;

    @Operation(summary = "Abre um chamado",
            description = "Solicitante vem do JWT; status ABERTO e dataAbertura são definidos pelo servidor.")
    @ApiResponse(responseCode = "201", description = "Chamado criado")
    @ApiResponse(responseCode = "400", description = "Título ou descrição inválidos")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @PostMapping
    public ResponseEntity<Chamado> abrir(@Valid @RequestBody ChamadoRequest request,
                                         Authentication auth) {
        Chamado criado = service.criar(request, auth.getName());
        return ResponseEntity
                .created(URI.create("/api/chamados/" + criado.getId()))
                .body(criado);
    }

    @Operation(summary = "Lista os chamados de um usuário",
            description = "Usuário comum só consulta os próprios; TECNICO e ADMIN consultam qualquer um.")
    @ApiResponse(responseCode = "200", description = "Lista de chamados (mais recentes primeiro)")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Sem permissão para ver chamados deste usuário")
    @GetMapping("/usuario/{id}")
    public ResponseEntity<List<Chamado>> listarPorUsuario(@PathVariable("id") String usuario,
                                                          Authentication auth) {
        return ResponseEntity.ok(service.listarPorUsuario(usuario, auth));
    }

    @Operation(summary = "Consulta um chamado", description = "Retorna detalhes e status do chamado.")
    @ApiResponse(responseCode = "200", description = "Chamado encontrado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Sem permissão para ver este chamado")
    @ApiResponse(responseCode = "404", description = "Chamado inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<Chamado> buscar(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(service.buscar(id, auth));
    }

    @Operation(summary = "Resolve um chamado",
            description = "Apenas TECNICO ou ADMIN. Grava solução, técnico, status RESOLVIDO e dataResolucao.")
    @ApiResponse(responseCode = "200", description = "Chamado resolvido")
    @ApiResponse(responseCode = "400", description = "Solução inválida")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Usuário sem papel de técnico")
    @ApiResponse(responseCode = "404", description = "Chamado inexistente")
    @ApiResponse(responseCode = "409", description = "Chamado já resolvido (imutável)")
    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    public ResponseEntity<Chamado> resolver(@PathVariable String id,
                                            @Valid @RequestBody ResolucaoRequest request,
                                            Authentication auth) {
        return ResponseEntity.ok(service.resolver(id, request, auth.getName()));
    }
}
