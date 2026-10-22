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

// aqui ficam as regras de negócio: quem vê o quê e o que o servidor define
// e a regra de que chamado resolvido não muda mais
@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository repository;

    // abre e salva o chamado novo
    // status aberto e data de abertura são definidos dentro do abrir da entidade
    // o solicitante é o usuario do JWT
    public Chamado criar(ChamadoRequest request, String solicitante) {
        return repository.save(
                Chamado.abrir(request.titulo(), request.descricao(), solicitante));
    }

    // lista os chamados de um usuario, do mais recente para o mais antigo
    // usuario comum só vê os próprios, tecnico e admin veem de qualquer um
    public List<Chamado> listarPorUsuario(String usuario, Authentication auth) {
        // não é o dono e também não é tecnico nem admin: 403
        if (!usuario.equals(auth.getName()) && !PapelUtil.isPrivilegiado(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode consultar os seus próprios chamados");
        }
        return repository.findBySolicitanteOrderByDataAberturaDesc(usuario);
    }

    // busca um chamado pelo id
    // 404 se não existir, 403 se não for o dono nem tecnico ou admin
    public Chamado buscar(String id, Authentication auth) {
        Chamado chamado = obter(id);

        if (!chamado.getSolicitante().equals(auth.getName()) && !PapelUtil.isPrivilegiado(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para ver este chamado");
        }
        return chamado;
    }

    // resolve o chamado: grava solução, tecnico, status resolvido e a data
    // a permissão de tecnico já foi checada no controller
    public Chamado resolver(String id, ResolucaoRequest request, String tecnico) {
        Chamado chamado = obter(id);

        // regra da imutabilidade: chamado já resolvido não muda mais
        // lança o 409 e não salva nada
        if (chamado.getStatus() == Status.RESOLVIDO) {
            throw new ChamadoJaResolvidoException(id);
        }

        chamado.registrarResolucao(request.solucao(), tecnico);
        return repository.save(chamado);
    }

    // busca o chamado ou lança 404
    // o buscar e o resolver usam o mesmo para não repetir código
    private Chamado obter(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Chamado não encontrado"));
    }
}
