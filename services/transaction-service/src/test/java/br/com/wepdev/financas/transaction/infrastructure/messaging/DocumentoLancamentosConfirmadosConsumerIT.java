package br.com.wepdev.financas.transaction.infrastructure.messaging;

import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoFiltro;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.infrastructure.client.AccountServiceClientImpl;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Publica um evento JSON real no tópico "documento.lancamentos-confirmados"
 * via Kafka Dev Services (Testcontainers, mesmo broker que o consumer real
 * está ouvindo) — valida a fiação de ponta a ponta (deserializer, consumer,
 * caso de uso), não só a lógica isolada. account-service é mockado (mesmo
 * padrão do CartaoResourceTest no card-service).
 */
@QuarkusTest
class DocumentoLancamentosConfirmadosConsumerIT {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @Inject
    TransacaoRepository transacaoRepository;

    private AccountServiceClientImpl accountServiceClientMock;

    @BeforeEach
    void setUp() {
        accountServiceClientMock = mock(AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(accountServiceClientMock, AccountServiceClientImpl.class);
    }

    @Test
    void deveriaConsumirEventoReal_eCriarTransacao() {
        UUID usuarioId = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        String json = """
                {"documentoId":"%s","usuarioId":"%s","contaId":"%s",\
                "lancamentos":[{"lancamentoId":"%s","descricao":"Mercado","valor":50.00,\
                "tipo":"DESPESA","categoria":"Alimentação","data":"2026-08-05"}],\
                "confirmadoEm":"2026-08-09T10:00:00Z"}
                """.formatted(UUID.randomUUID(), usuarioId, contaId, UUID.randomUUID());

        Map<String, Object> config = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()
        );
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(config)) {
            producer.send(new ProducerRecord<>("documento.lancamentos-confirmados", json));
        }

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            // listar() precisa de transação/contexto ativo (Hibernate ORM Panache) — o corpo do teste
            // não tem nenhum dos dois por padrão, só requests HTTP reais têm (mesmo motivo do
            // ContextNotActiveException já visto no document-service com o ManagedExecutor).
            List<Transacao> transacoes = QuarkusTransaction.requiringNew()
                    .call(() -> transacaoRepository.listar(new TransacaoFiltro(usuarioId, null, null, null)));
            assertThat(transacoes).hasSize(1);
            assertThat(transacoes.get(0).getDescricao()).isEqualTo("Mercado");
            assertThat(transacoes.get(0).getValor()).isEqualByComparingTo("50.00");
            assertThat(transacoes.get(0).getContaId()).isEqualTo(contaId);
        });

        verify(accountServiceClientMock).debitarSemConfirmarPosse(contaId, new BigDecimal("50.00"));
        verify(accountServiceClientMock, org.mockito.Mockito.never()).debitar(any(), any());
    }
}
