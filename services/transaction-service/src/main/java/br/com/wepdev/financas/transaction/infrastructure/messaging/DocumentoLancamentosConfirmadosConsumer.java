package br.com.wepdev.financas.transaction.infrastructure.messaging;

import br.com.wepdev.financas.transaction.application.LancamentoConfirmadoCommand;
import br.com.wepdev.financas.transaction.application.ProcessarLancamentosConfirmadosCommand;
import br.com.wepdev.financas.transaction.application.ProcessarLancamentosConfirmadosUseCase;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class DocumentoLancamentosConfirmadosConsumer {

    private final ProcessarLancamentosConfirmadosUseCase useCase;

    public DocumentoLancamentosConfirmadosConsumer(ProcessarLancamentosConfirmadosUseCase useCase) {
        this.useCase = useCase;
    }

    @Incoming("documento-lancamentos-confirmados")
    public void consumir(DocumentoLancamentosConfirmadosEvento evento) {
        var lancamentos = evento.lancamentos().stream()
                .map(l -> new LancamentoConfirmadoCommand(
                        l.descricao(),
                        l.valor(),
                        TipoTransacao.valueOf(l.tipo()),
                        l.categoria(),
                        l.data()
                ))
                .toList();

        useCase.executar(new ProcessarLancamentosConfirmadosCommand(evento.usuarioId(), evento.contaId(), lancamentos));
    }
}
