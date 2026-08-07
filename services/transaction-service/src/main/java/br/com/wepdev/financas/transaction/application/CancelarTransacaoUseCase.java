package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class CancelarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final AccountServiceClient accountServiceClient;

    public CancelarTransacaoUseCase(TransacaoRepository transacaoRepository, AccountServiceClient accountServiceClient) {
        this.transacaoRepository = transacaoRepository;
        this.accountServiceClient = accountServiceClient;
    }

    /**
     * Idempotente: cancelar de novo uma transação já cancelada não chama o
     * account-service de novo (evitaria reverter o saldo duas vezes).
     * Reverte o efeito original: DESPESA (que debitou) credita de volta,
     * RECEITA (que creditou) debita de volta — se a conta não tiver saldo
     * suficiente pra reverter uma RECEITA, falha com 422 (o dinheiro já foi
     * gasto em outro lugar) e a transação continua CONFIRMADA.
     */
    @Transactional
    public void executar(UUID id, UUID usuarioId) {
        Transacao transacao = transacaoRepository.buscarPorId(id)
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new TransacaoNaoEncontradaException(id));

        if (transacao.isCancelada()) {
            return;
        }

        if (transacao.getTipo() == TipoTransacao.DESPESA) {
            accountServiceClient.creditar(transacao.getContaId(), transacao.getValor());
        } else {
            accountServiceClient.debitar(transacao.getContaId(), transacao.getValor());
        }

        transacao.cancelar();
        transacaoRepository.salvar(transacao);
    }
}
