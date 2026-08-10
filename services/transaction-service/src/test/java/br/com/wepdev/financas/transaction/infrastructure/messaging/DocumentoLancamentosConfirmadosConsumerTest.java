package br.com.wepdev.financas.transaction.infrastructure.messaging;

import br.com.wepdev.financas.transaction.application.ProcessarLancamentosConfirmadosCommand;
import br.com.wepdev.financas.transaction.application.ProcessarLancamentosConfirmadosUseCase;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DocumentoLancamentosConfirmadosConsumerTest {

    private final ProcessarLancamentosConfirmadosUseCase useCase = mock(ProcessarLancamentosConfirmadosUseCase.class);
    private final DocumentoLancamentosConfirmadosConsumer consumer = new DocumentoLancamentosConfirmadosConsumer(useCase);

    @Test
    void deveriaMapearEventoParaComando_eDelegarAoCasoDeUso() {
        UUID usuarioId = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        var payload = new LancamentoConfirmadoPayload(UUID.randomUUID(), "Mercado", new BigDecimal("50.00"),
                "DESPESA", "Alimentação", LocalDate.of(2026, 8, 5));
        var evento = new DocumentoLancamentosConfirmadosEvento(UUID.randomUUID(), usuarioId, contaId,
                List.of(payload), Instant.now());

        consumer.consumir(evento);

        var captor = forClass(ProcessarLancamentosConfirmadosCommand.class);
        verify(useCase).executar(captor.capture());
        ProcessarLancamentosConfirmadosCommand comando = captor.getValue();
        assertThat(comando.usuarioId()).isEqualTo(usuarioId);
        assertThat(comando.contaId()).isEqualTo(contaId);
        assertThat(comando.lancamentos()).hasSize(1);
        assertThat(comando.lancamentos().get(0).descricao()).isEqualTo("Mercado");
        assertThat(comando.lancamentos().get(0).valor()).isEqualByComparingTo("50.00");
        assertThat(comando.lancamentos().get(0).tipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(comando.lancamentos().get(0).categoria()).isEqualTo("Alimentação");
        assertThat(comando.lancamentos().get(0).data()).isEqualTo(LocalDate.of(2026, 8, 5));
    }
}
