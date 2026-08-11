package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoEventPublisher;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Consumido pelo {@code DocumentoLancamentosConfirmadosConsumer} (Kafka,
 * tópico "documento.lancamentos-confirmados") — cria uma {@link Transacao}
 * avulsa comum por item da lista recebida (ADR-0023, sem integração com
 * card-service). Loop preparado pra N itens, mas na prática o
 * document-service publica sempre 1 (a despesa consolidada da fatura
 * inteira, 2026-08-11 — ver {@code DocumentoImportado.getDespesaConsolidada}
 * no document-service): fatura nunca vira uma transação por lançamento.
 * Usa {@code debitarSemConfirmarPosse}/
 * {@code creditarSemConfirmarPosse} (ADR-0025): posse de {@code contaId} já
 * foi confirmada no document-service, síncrono, antes do evento ser
 * publicado — este consumer não tem token de usuário pra reverificar.
 *
 * <p>Limitação conhecida, aceita pro MVP (mesmo nível dos outros fluxos
 * desse projeto — sem outbox/idempotency-key ainda): se o processo cair
 * entre o débito no account-service e o commit da Transacao, uma
 * redelivery do Kafka (semântica at-least-once) causaria débito
 * duplicado. Revisitar se isso virar problema real em produção.
 */
@ApplicationScoped
public class ProcessarLancamentosConfirmadosUseCase {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoEventPublisher eventPublisher;
    private final AccountServiceClient accountServiceClient;

    public ProcessarLancamentosConfirmadosUseCase(TransacaoRepository transacaoRepository,
                                                    TransacaoEventPublisher eventPublisher,
                                                    AccountServiceClient accountServiceClient) {
        this.transacaoRepository = transacaoRepository;
        this.eventPublisher = eventPublisher;
        this.accountServiceClient = accountServiceClient;
    }

    @Transactional
    public void executar(ProcessarLancamentosConfirmadosCommand command) {
        for (LancamentoConfirmadoCommand lancamento : command.lancamentos()) {
            if (lancamento.tipo() == TipoTransacao.DESPESA) {
                accountServiceClient.debitarSemConfirmarPosse(command.contaId(), lancamento.valor());
            } else {
                accountServiceClient.creditarSemConfirmarPosse(command.contaId(), lancamento.valor());
            }

            Transacao transacao = Transacao.criar(
                    command.contaId(),
                    command.usuarioId(),
                    lancamento.descricao(),
                    lancamento.valor(),
                    lancamento.tipo(),
                    lancamento.categoria(),
                    lancamento.data()
            );
            transacaoRepository.salvar(transacao);
            eventPublisher.publicarTransacaoRegistrada(transacao);
        }
    }
}
