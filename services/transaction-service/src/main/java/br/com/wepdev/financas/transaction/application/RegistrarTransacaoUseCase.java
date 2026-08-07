package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoEventPublisher;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RegistrarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoEventPublisher eventPublisher;
    private final AccountServiceClient accountServiceClient;

    public RegistrarTransacaoUseCase(TransacaoRepository transacaoRepository, TransacaoEventPublisher eventPublisher,
                                      AccountServiceClient accountServiceClient) {
        this.transacaoRepository = transacaoRepository;
        this.eventPublisher = eventPublisher;
        this.accountServiceClient = accountServiceClient;
    }

    /**
     * Chama o account-service ANTES de persistir — se debitar/creditar falhar
     * (conta não encontrada, saldo insuficiente, indisponibilidade), a
     * exceção propaga e nada é salvo (sem transação "fantasma").
     */
    @Transactional
    public Transacao executar(RegistrarTransacaoCommand command) {
        if (command.tipo() == TipoTransacao.DESPESA) {
            accountServiceClient.debitar(command.contaId(), command.valor());
        } else {
            accountServiceClient.creditar(command.contaId(), command.valor());
        }

        Transacao transacao = Transacao.criar(
                command.contaId(),
                command.usuarioId(),
                command.descricao(),
                command.valor(),
                command.tipo(),
                command.categoria(),
                command.dataTransacao()
        );
        transacaoRepository.salvar(transacao);
        eventPublisher.publicarTransacaoRegistrada(transacao);
        return transacao;
    }
}
