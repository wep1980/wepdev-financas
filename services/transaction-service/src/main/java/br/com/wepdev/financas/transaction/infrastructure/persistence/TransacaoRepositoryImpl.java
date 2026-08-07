package br.com.wepdev.financas.transaction.infrastructure.persistence;

import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoFiltro;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TransacaoRepositoryImpl implements TransacaoRepository {

    private final TransacaoPanacheRepository panacheRepository;

    public TransacaoRepositoryImpl(TransacaoPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void salvar(Transacao transacao) {
        TransacaoEntity entity = panacheRepository.findById(transacao.getId());
        if (entity == null) {
            panacheRepository.persist(TransacaoMapper.paraNovaEntidade(transacao));
        } else {
            TransacaoMapper.atualizarEntidade(entity, transacao);
        }
    }

    @Override
    public Optional<Transacao> buscarPorId(UUID id) {
        return panacheRepository.findByIdOptional(id).map(TransacaoMapper::paraDominio);
    }

    @Override
    public List<Transacao> listar(TransacaoFiltro filtro) {
        StringBuilder query = new StringBuilder("usuarioId = :usuarioId");
        Map<String, Object> params = new HashMap<>();
        params.put("usuarioId", filtro.usuarioId());

        if (filtro.contaId() != null) {
            query.append(" and contaId = :contaId");
            params.put("contaId", filtro.contaId());
        }
        if (filtro.inicio() != null) {
            query.append(" and dataTransacao >= :inicio");
            params.put("inicio", filtro.inicio());
        }
        if (filtro.fim() != null) {
            query.append(" and dataTransacao <= :fim");
            params.put("fim", filtro.fim());
        }
        query.append(" order by dataTransacao desc, criadoEm desc");

        return panacheRepository.list(query.toString(), params).stream()
                .map(TransacaoMapper::paraDominio)
                .toList();
    }
}
