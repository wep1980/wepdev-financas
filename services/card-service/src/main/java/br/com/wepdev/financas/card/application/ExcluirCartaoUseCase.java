package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class ExcluirCartaoUseCase {

    private final CartaoRepository cartaoRepository;

    public ExcluirCartaoUseCase(CartaoRepository cartaoRepository) {
        this.cartaoRepository = cartaoRepository;
    }

    /** Idempotente: excluir de novo um cartão já inativo continua sem erro. Não afeta faturas/compras já existentes. */
    @Transactional
    public void executar(UUID id, UUID usuarioId) {
        Cartao cartao = cartaoRepository.buscarPorId(id)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new CartaoNaoEncontradoException(id));

        if (!cartao.isAtivo()) {
            return;
        }

        cartao.inativar();
        cartaoRepository.salvar(cartao);
    }
}
