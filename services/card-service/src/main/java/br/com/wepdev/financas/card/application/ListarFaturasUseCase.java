package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.StatusFatura;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListarFaturasUseCase {

    private final CartaoRepository cartaoRepository;
    private final FaturaRepository faturaRepository;

    public ListarFaturasUseCase(CartaoRepository cartaoRepository, FaturaRepository faturaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.faturaRepository = faturaRepository;
    }

    public List<Fatura> executar(UUID cartaoId, UUID usuarioId, StatusFatura status) {
        cartaoRepository.buscarPorId(cartaoId)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new CartaoNaoEncontradoException(cartaoId));

        return faturaRepository.listarPorCartao(cartaoId, status);
    }
}
