package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.AccountServiceClient;
import br.com.wepdev.financas.document.domain.DocumentoEventPublisher;
import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.NenhumLancamentoSelecionadoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Idempotente: confirmar de novo um documento já CONFIRMADO não publica
 * evento de novo (a checagem de status acontece ANTES de chamar
 * {@code documento.confirmar()}, que por si só também é idempotente — a
 * checagem aqui é sobre se DEVE publicar o evento, não sobre validar a
 * transição em si, que já é responsabilidade do domínio).
 *
 * <p>Confirma posse de {@code contaId} contra o account-service ANTES de
 * publicar o evento (ADR-0025) — é aqui, numa requisição HTTP síncrona com
 * o token do usuário disponível, que essa verificação é possível; o
 * consumer Kafka no transaction-service não tem token pra fazer isso, então
 * confia integralmente nessa checagem já ter acontecido.
 */
@ApplicationScoped
public class ConfirmarLancamentosUseCase {

    private final DocumentoRepository repository;
    private final DocumentoEventPublisher eventPublisher;
    private final AccountServiceClient accountServiceClient;

    public ConfirmarLancamentosUseCase(DocumentoRepository repository, DocumentoEventPublisher eventPublisher,
                                        AccountServiceClient accountServiceClient) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.accountServiceClient = accountServiceClient;
    }

    @Transactional
    public void executar(ConfirmarLancamentosCommand command) {
        DocumentoImportado documento = repository.buscarPorId(command.documentoId(), command.usuarioId())
                .orElseThrow(() -> new DocumentoNaoEncontradoException(command.documentoId()));

        if (documento.isConfirmado()) {
            return;
        }
        if (command.lancamentoIdsConfirmados().isEmpty()) {
            throw new NenhumLancamentoSelecionadoException();
        }
        accountServiceClient.confirmarPosseDaConta(command.contaId());

        documento.confirmar(command.lancamentoIdsConfirmados());
        repository.salvar(documento);
        eventPublisher.publicarLancamentosConfirmados(documento, command.contaId());
    }
}
