package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListarCartoesUseCase {

    private final CartaoRepository cartaoRepository;

    public ListarCartoesUseCase(CartaoRepository cartaoRepository) {
        this.cartaoRepository = cartaoRepository;
    }

    public List<Cartao> executar(UUID usuarioId) {
        return cartaoRepository.listarAtivos(usuarioId);
    }
}
