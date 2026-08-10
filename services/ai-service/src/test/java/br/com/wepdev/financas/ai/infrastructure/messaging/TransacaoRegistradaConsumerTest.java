package br.com.wepdev.financas.ai.infrastructure.messaging;

import br.com.wepdev.financas.ai.application.IndexarTransacaoComando;
import br.com.wepdev.financas.ai.application.IndexarTransacaoUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransacaoRegistradaConsumerTest {

    private final IndexarTransacaoUseCase useCase = mock(IndexarTransacaoUseCase.class);
    private final TransacaoRegistradaConsumer consumer = new TransacaoRegistradaConsumer(useCase);

    @Test
    void deveriaMapearEventoParaComando_eDelegarAoCasoDeUso() {
        UUID transacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        var evento = new TransacaoRegistradaEvento(transacaoId, UUID.randomUUID(), usuarioId, "Supermercado",
                "Alimentação", "DESPESA", new BigDecimal("50.00"), Instant.now());

        consumer.consumir(evento);

        var captor = forClass(IndexarTransacaoComando.class);
        verify(useCase).executar(captor.capture());
        IndexarTransacaoComando comando = captor.getValue();
        assertThat(comando.transacaoId()).isEqualTo(transacaoId);
        assertThat(comando.usuarioId()).isEqualTo(usuarioId);
        assertThat(comando.descricao()).isEqualTo("Supermercado");
    }
}
