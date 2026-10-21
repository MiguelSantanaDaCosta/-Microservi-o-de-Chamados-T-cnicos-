package com.santana.chamados.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.santana.chamados.dto.ChamadoRequest;
import com.santana.chamados.entity.Chamado;
import com.santana.chamados.enums.Status;
import com.santana.chamados.repository.ChamadoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository repository;

    public Chamado criar(ChamadoRequest request, String solicitante) {
        Chamado chamado = Chamado.builder()
                .titulo(request.titulo().trim())
                .descricao(request.descricao().trim())
                .solicitante(solicitante)
                .status(Status.ABERTO)
                .dataAbertura(Instant.now())
                .build();
        return repository.save(chamado);
    }

    public List<Chamado> listarPorUsuario(String usuario, Authentication auth) {
        if (!usuario.equals(auth.getName()) && !isPrivilegiado(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode consultar os seus próprios chamados");
        }
        return repository.findBySolicitanteOrderByDataAberturaDesc(usuario);
    }

    public Chamado buscar(String id, Authentication auth) {
				Chamado chamado = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
				if (!chamado.getSolicitante().equals(auth.getName()) && !isPrivilegiado(auth)) {
					throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para ver este chamado");
				}
        return chamado;
    }

    private boolean isPrivilegiado(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECNICO")
                || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
