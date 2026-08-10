package br.com.wepdev.financas.document.infrastructure.messaging;

import br.com.wepdev.financas.document.domain.DocumentoEventPublisher;
import br.com.wepdev.financas.document.domain.DocumentoImportado;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class DocumentoEventPublisherImpl implements DocumentoEventPublisher {

    @Channel("documento-lancamentos-confirmados")
    Emitter<DocumentoLancamentosConfirmadosEvento> emitter;

    @Override
    public void publicarLancamentosConfirmados(DocumentoImportado documento, UUID contaId) {
        var lancamentos = documento.getLancamentosConfirmados().stream()
                .map(l -> new LancamentoConfirmadoPayload(
                        l.getId(),
                        l.getDescricao(),
                        l.getValor(),
                        l.getTipo().name(),
                        l.getCategoriaSugerida(),
                        l.getData()
                ))
                .toList();

        emitter.send(new DocumentoLancamentosConfirmadosEvento(
                documento.getId(),
                documento.getUsuarioId(),
                contaId,
                lancamentos,
                Instant.now()
        ));
    }
}
