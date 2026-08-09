package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarCartaoUseCase {

    private final CartaoRepository cartaoRepository;

    public BuscarCartaoUseCase(CartaoRepository cartaoRepository) {
        this.cartaoRepository = cartaoRepository;
    }

    public Cartao executar(UUID id, UUID usuarioId) {
        return cartaoRepository.buscarPorId(id)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new CartaoNaoEncontradoException(id));
    }
}
