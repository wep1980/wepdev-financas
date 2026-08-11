package br.com.wepdev.financas.document.infrastructure.messaging;

import br.com.wepdev.financas.document.domain.DocumentoEventPublisher;
import br.com.wepdev.financas.document.domain.DocumentoImportado;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.Instant;
import java.util.UUID;

/**
 * Sem chamador hoje (ADR-0028, 2026-08-11) — FATURA_CARTAO (único tipo
 * implementado) passou a confirmar lançando compras no card-service, não
 * publicando evento. Mantido pronto pra EXTRATO_BANCARIO/BOLETO_FINANCIAMENTO
 * (ADR-0023/0028), que devem continuar usando esse caminho — cada tipo
 * de documento decide sua própria forma de agregação quando for
 * implementado, aqui fica só o transporte (1 lançamento confirmado = 1
 * item no evento, mesma forma consumida por
 * {@code DocumentoLancamentosConfirmadosConsumer} no transaction-service).
 */
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
