package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.AccountServiceClient;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaAindaAbertaException;
import br.com.wepdev.financas.card.domain.FaturaNaoEncontradaException;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class PagarFaturaUseCase {

    private final FaturaRepository faturaRepository;
    private final CartaoRepository cartaoRepository;
    private final AccountServiceClient accountServiceClient;

    public PagarFaturaUseCase(FaturaRepository faturaRepository, CartaoRepository cartaoRepository,
                               AccountServiceClient accountServiceClient) {
        this.faturaRepository = faturaRepository;
        this.cartaoRepository = cartaoRepository;
        this.accountServiceClient = accountServiceClient;
    }

    /**
     * Idempotente: pagar de novo uma fatura já PAGA não debita de novo.
     * Só permite pagar fatura FECHADA — ABERTA não tem valorTotal
     * definitivo ainda (422). Debita ANTES de marcar PAGA — se o débito
     * falhar, a fatura continua FECHADA, sem "pagamento fantasma".
     */
    @Transactional
    public void executar(UUID id, UUID usuarioId) {
        Fatura fatura = faturaRepository.buscarPorId(id)
                .filter(f -> f.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new FaturaNaoEncontradaException(id));

        if (fatura.isPaga()) {
            return;
        }
        if (fatura.isAberta()) {
            throw new FaturaAindaAbertaException(id);
        }

        Cartao cartao = cartaoRepository.buscarPorId(fatura.getCartaoId())
                .orElseThrow(() -> new CartaoNaoEncontradoException(fatura.getCartaoId()));

        accountServiceClient.debitar(cartao.getContaPagamentoId(), fatura.getValorTotal());

        fatura.pagar();
        faturaRepository.salvar(fatura);
    }
}
