package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoCanceladaException;
import br.com.wepdev.financas.transaction.domain.TransacaoNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class AtualizarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final AccountServiceClient accountServiceClient;

    public AtualizarTransacaoUseCase(TransacaoRepository transacaoRepository, AccountServiceClient accountServiceClient) {
        this.transacaoRepository = transacaoRepository;
        this.accountServiceClient = accountServiceClient;
    }

    /**
     * Se o valor mudou, ajusta o saldo pela DIFERENÇA líquida numa chamada só
     * ao account-service (não reverte-e-reaplica em duas chamadas) — evita
     * uma janela onde o efeito antigo já foi revertido mas o novo ainda não
     * foi aplicado (ou vice-versa) se a segunda chamada falhasse.
     */
    @Transactional
    public Transacao executar(AtualizarTransacaoCommand command) {
        Transacao transacao = transacaoRepository.buscarPorId(command.id())
                .filter(t -> t.getUsuarioId().equals(command.usuarioId()))
                .orElseThrow(() -> new TransacaoNaoEncontradaException(command.id()));

        if (transacao.isCancelada()) {
            throw new TransacaoCanceladaException(command.id());
        }

        BigDecimal deltaEfeito = efeitoNoSaldo(transacao.getTipo(), command.valor())
                .subtract(efeitoNoSaldo(transacao.getTipo(), transacao.getValor()));
        if (deltaEfeito.signum() > 0) {
            accountServiceClient.debitar(transacao.getContaId(), deltaEfeito);
        } else if (deltaEfeito.signum() < 0) {
            accountServiceClient.creditar(transacao.getContaId(), deltaEfeito.abs());
        }

        transacao.atualizar(command.descricao(), command.valor(), command.categoria(), command.dataTransacao());
        transacaoRepository.salvar(transacao);
        return transacao;
    }

    /** DESPESA debita (efeito positivo); RECEITA credita (efeito negativo, "debita ao contrário"). */
    private BigDecimal efeitoNoSaldo(TipoTransacao tipo, BigDecimal valor) {
        return tipo == TipoTransacao.DESPESA ? valor : valor.negate();
    }
}
