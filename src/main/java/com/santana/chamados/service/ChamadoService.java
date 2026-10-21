package com.santana.chamados.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.santana.chamados.enums.Status;
import com.santana.chamados.dto.ChamadoRequest;
import com.santana.chamados.dto.ResolucaoRequest;
import com.santana.chamados.entity.Chamado;
import com.santana.chamados.exception.ChamadoJaResolvidoException;
import com.santana.chamados.repository.ChamadoRepository;
import com.santana.chamados.security.PapelUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository repository;

    public Chamado criar(ChamadoRequest request, String solicitante) {
        return repository.save(
                Chamado.abrir(request.titulo(), request.descricao(), solicitante));
    }

    public List<Chamado> listarPorUsuario(String usuario, Authentication auth) {
        if (!usuario.equals(auth.getName()) && !PapelUtil.isPrivilegiado(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode consultar os seus próprios chamados");
        }
        return repository.findBySolicitanteOrderByDataAberturaDesc(usuario);
    }

    public Chamado buscar(String id, Authentication auth) {
        Chamado chamado = obter(id);

        if (!chamado.getSolicitante().equals(auth.getName()) && !PapelUtil.isPrivilegiado(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para ver este chamado");
        }
        return chamado;
    }

    public Chamado resolver(String id, ResolucaoRequest request, String tecnico) {
        Chamado chamado = obter(id);

        if (chamado.getStatus() == Status.RESOLVIDO) {
            throw new ChamadoJaResolvidoException(id);
        }

        chamado.registrarResolucao(request.solucao(), tecnico);
        return repository.save(chamado);
    }

    private Chamado obter(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Chamado não encontrado"));
    }
}
