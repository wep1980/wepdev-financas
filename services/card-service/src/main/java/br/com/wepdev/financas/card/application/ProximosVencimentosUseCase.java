package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

/** Consumido pelo job diário do notification-service (ADR-0010) — não filtra por usuarioId, ver spec. */
@ApplicationScoped
public class ProximosVencimentosUseCase {

    private final FaturaRepository faturaRepository;
    private final CartaoRepository cartaoRepository;

    public ProximosVencimentosUseCase(FaturaRepository faturaRepository, CartaoRepository cartaoRepository) {
        this.faturaRepository = faturaRepository;
        this.cartaoRepository = cartaoRepository;
    }

    public List<ProximoVencimentoFatura> executar(LocalDate hoje, int dias) {
        LocalDate limite = hoje.plusDays(dias);
        return faturaRepository.listarFechadas().stream()
                .filter(f -> !f.getDataVencimento().isBefore(hoje) && !f.getDataVencimento().isAfter(limite))
                .map(this::paraProximoVencimento)
                .toList();
    }

    private ProximoVencimentoFatura paraProximoVencimento(Fatura fatura) {
        String apelidoCartao = cartaoRepository.buscarPorId(fatura.getCartaoId())
                .map(Cartao::getApelido)
                .orElse(null);
        return new ProximoVencimentoFatura(fatura.getId(), fatura.getCartaoId(), fatura.getUsuarioId(),
                apelidoCartao, fatura.getValorTotal(), fatura.getDataVencimento());
    }
}
