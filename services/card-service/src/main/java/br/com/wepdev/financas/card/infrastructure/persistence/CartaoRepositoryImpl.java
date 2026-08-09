package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CartaoRepositoryImpl implements CartaoRepository {

    private final CartaoPanacheRepository panacheRepository;

    public CartaoRepositoryImpl(CartaoPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void salvar(Cartao cartao) {
        CartaoEntity entity = panacheRepository.findById(cartao.getId());
        if (entity == null) {
            panacheRepository.persist(CartaoMapper.paraNovaEntidade(cartao));
        } else {
            CartaoMapper.atualizarEntidade(entity, cartao);
        }
    }

    @Override
    public Optional<Cartao> buscarPorId(UUID id) {
        return panacheRepository.findByIdOptional(id).map(CartaoMapper::paraDominio);
    }

    @Override
    public List<Cartao> listarAtivos(UUID usuarioId) {
        return panacheRepository.list("usuarioId = ?1 and ativo = true order by criadoEm desc", usuarioId).stream()
                .map(CartaoMapper::paraDominio)
                .toList();
    }
}
