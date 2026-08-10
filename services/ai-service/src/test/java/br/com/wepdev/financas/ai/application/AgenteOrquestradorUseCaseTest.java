package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.AccountServiceClient;
import br.com.wepdev.financas.ai.domain.BudgetServiceClient;
import br.com.wepdev.financas.ai.domain.CardServiceClient;
import br.com.wepdev.financas.ai.domain.Cartao;
import br.com.wepdev.financas.ai.domain.ChatRequest;
import br.com.wepdev.financas.ai.domain.ChatResponse;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIaRepository;
import br.com.wepdev.financas.ai.domain.Conta;
import br.com.wepdev.financas.ai.domain.Conversa;
import br.com.wepdev.financas.ai.domain.ConversaNaoEncontradaException;
import br.com.wepdev.financas.ai.domain.ConversaRepository;
import br.com.wepdev.financas.ai.domain.DisponivelParaGastar;
import br.com.wepdev.financas.ai.domain.Fatura;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.domain.LlmProviderFactory;
import br.com.wepdev.financas.ai.domain.ResultadoBusca;
import br.com.wepdev.financas.ai.domain.ResumoCategoria;
import br.com.wepdev.financas.ai.domain.StatusFatura;
import br.com.wepdev.financas.ai.domain.TipoRespostaAgente;
import br.com.wepdev.financas.ai.domain.TransactionServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgenteOrquestradorUseCaseTest {

    private final ConversaRepository conversaRepository = mock(ConversaRepository.class);
    private final ConfiguracaoIaRepository configuracaoIaRepository = mock(ConfiguracaoIaRepository.class);
    private final LlmProviderFactory llmProviderFactory = mock(LlmProviderFactory.class);
    private final LlmProvider llmProvider = mock(LlmProvider.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final BudgetServiceClient budgetServiceClient = mock(BudgetServiceClient.class);
    private final CardServiceClient cardServiceClient = mock(CardServiceClient.class);
    private final TransactionServiceClient transactionServiceClient = mock(TransactionServiceClient.class);
    private final BuscarTransacoesSimilaresUseCase buscarTransacoesSimilaresUseCase = mock(BuscarTransacoesSimilaresUseCase.class);

    private AgenteOrquestradorUseCase useCase;

    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new AgenteOrquestradorUseCase(conversaRepository, configuracaoIaRepository, llmProviderFactory,
                accountServiceClient, budgetServiceClient, cardServiceClient, transactionServiceClient,
                buscarTransacoesSimilaresUseCase, new ObjectMapper());
        when(configuracaoIaRepository.buscarPorUsuario(usuarioId)).thenReturn(Optional.empty());
        when(llmProviderFactory.criar(any())).thenReturn(llmProvider);
    }

    private void mockConversaNova() {
        when(conversaRepository.buscarPorId(any())).thenReturn(Optional.empty());
    }

    @Test
    void deveriaResponderSaldoDisponivel_quandoConsultaClassificada() {
        mockConversaNova();
        when(llmProvider.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"buscar_saldo_disponivel\", \"periodo\": null}"));
        when(budgetServiceClient.buscarDisponivelParaGastar(YearMonth.now()))
                .thenReturn(new DisponivelParaGastar(new BigDecimal("800.00"), new BigDecimal("3000.00"),
                        new BigDecimal("500.00"), new BigDecimal("1500.00"), new BigDecimal("200.00")));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "quanto posso gastar esse mês?"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
        assertThat(resultado.resposta()).contains("800.00");
        assertThat(resultado.trace()).hasSize(1);
        assertThat(resultado.trace().get(0).nome()).isEqualTo("buscar_saldo_disponivel");
        verify(conversaRepository).salvar(any());
    }

    @Test
    void deveriaResponderResumoPorCategoria_quandoConsultaClassificada() {
        mockConversaNova();
        when(llmProvider.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"resumo_gastos_por_categoria\", \"periodo\": \"MES_ATUAL\"}"));
        when(transactionServiceClient.buscarResumoPorCategoria(any(), any()))
                .thenReturn(List.of(new ResumoCategoria("Mercado", new BigDecimal("300.00"), new BigDecimal("50.00"), null)));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "quanto gastei com mercado?"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
        assertThat(resultado.resposta()).contains("Mercado").contains("300.00");
    }

    @Test
    void deveriaCompararComPeriodoAnterior_quandoTransactionServiceDevolveOsDoisValores() {
        mockConversaNova();
        when(llmProvider.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"resumo_gastos_por_categoria\", \"periodo\": \"MES_ATUAL\"}"));
        when(transactionServiceClient.buscarResumoPorCategoria(any(), any())).thenReturn(List.of(
                new ResumoCategoria("Mercado", new BigDecimal("300.00"), new BigDecimal("60.00"), new BigDecimal("200.00")),
                new ResumoCategoria("Transporte", new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("150.00"))));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "gastei mais que o mês passado?"));

        // total atual 400.00 > total anterior 350.00 -> "maior"
        assertThat(resultado.resposta()).containsIgnoringCase("maior").contains("400.00").contains("350.00");
    }

    @Test
    void deveriaResponderFaturaCartao_quandoTemCartao() {
        mockConversaNova();
        when(llmProvider.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"buscar_fatura_cartao\", \"periodo\": null}"));
        UUID cartaoId = UUID.randomUUID();
        when(cardServiceClient.buscarCartoesAtivos()).thenReturn(List.of(new Cartao(cartaoId, "Nubank")));
        when(cardServiceClient.buscarFaturas(cartaoId, StatusFatura.FECHADA)).thenReturn(List.of(
                new Fatura(UUID.randomUUID(), YearMonth.now(), LocalDate.of(2026, 9, 10), new BigDecimal("450.00"), StatusFatura.FECHADA)));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "quando vence minha fatura?"));

        assertThat(resultado.resposta()).contains("Nubank").contains("450.00");
    }

    @Test
    void deveriaResponderSemCartao_quandoNaoTemNenhum() {
        mockConversaNova();
        when(llmProvider.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"buscar_fatura_cartao\", \"periodo\": null}"));
        when(cardServiceClient.buscarCartoesAtivos()).thenReturn(List.of());

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "quando vence minha fatura?"));

        assertThat(resultado.resposta()).containsIgnoringCase("não tem cartão");
    }

    @Test
    void deveriaResponderTransacoesSimilares_viaBuscaSemantica() {
        mockConversaNova();
        when(llmProvider.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"buscar_transacoes\", \"periodo\": null}"));
        when(buscarTransacoesSimilaresUseCase.executar(eq(usuarioId), any(), eq(5)))
                .thenReturn(List.of(new ResultadoBusca(UUID.randomUUID(), "Supermercado Pão de Açúcar", 0.9f)));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "gastos com mercado"));

        assertThat(resultado.resposta()).contains("Supermercado Pão de Açúcar");
    }

    @Test
    void deveriaProporAcao_quandoContaResolvidaComSucesso() {
        mockConversaNova();
        UUID contaId = UUID.randomUUID();
        when(llmProvider.chat(any()))
                .thenReturn(new ChatResponse("{\"intent\": \"ACAO\"}"))
                .thenReturn(new ChatResponse("""
                        {"tipo": "DESPESA", "descricao": "Aluguel", "valor": 1500.00, "recorrente": true,
                         "quantidadeOcorrencias": null, "categoria": "Moradia", "contaTexto": "conta corrente"}
                        """));
        when(accountServiceClient.buscarContasAtivas()).thenReturn(List.of(new Conta(contaId, "Conta Corrente")));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null,
                "criar uma despesa recorrente de aluguel de R$1500 na conta corrente"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.PROPOSTA_ACAO);
        assertThat(resultado.acaoProposta()).isNotNull();
        assertThat(resultado.acaoProposta().getContaId()).isEqualTo(contaId);
        assertThat(resultado.acaoProposta().getValor()).isEqualByComparingTo("1500.00");
        assertThat(resultado.resposta()).contains("Confirma?");
        verify(transactionServiceClient, never()).criarTransacao(any());
        verify(transactionServiceClient, never()).criarTransacaoRecorrente(any());
    }

    @Test
    void deveriaPedirConta_quandoContaNaoIdentificada() {
        mockConversaNova();
        when(llmProvider.chat(any()))
                .thenReturn(new ChatResponse("{\"intent\": \"ACAO\"}"))
                .thenReturn(new ChatResponse("""
                        {"tipo": "RECEITA", "descricao": "Salário", "valor": 10000.00, "recorrente": true,
                         "quantidadeOcorrencias": null, "categoria": "Salário", "contaTexto": null}
                        """));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "adicione uma receita mensal de 10 mil"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
        assertThat(resultado.acaoProposta()).isNull();
        assertThat(resultado.resposta()).containsIgnoringCase("qual conta");
    }

    @Test
    void deveriaPedirReformulacao_quandoExtracaoDeAcaoIncompleta() {
        mockConversaNova();
        when(llmProvider.chat(any()))
                .thenReturn(new ChatResponse("{\"intent\": \"ACAO\"}"))
                .thenReturn(new ChatResponse("{\"tipo\": null, \"descricao\": null, \"valor\": null}"));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "cria uma despesa aí"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
        assertThat(resultado.resposta()).containsIgnoringCase("não consegui entender");
    }

    @Test
    void deveriaResponderDesconhecida_quandoIntentNaoReconhecido() {
        mockConversaNova();
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("{\"intent\": \"DESCONHECIDA\"}"));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, null, "oi tudo bem?"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
        assertThat(resultado.resposta()).containsIgnoringCase("não entendi");
    }

    @Test
    void deveriaConfirmarEExecutarAcaoPontual_quandoUsuarioConfirma() {
        UUID contaId = UUID.randomUUID();
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.proporAcao(br.com.wepdev.financas.ai.domain.AcaoPendente.propor(
                br.com.wepdev.financas.ai.domain.TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"),
                false, null, null, contaId, "Alimentação"));
        when(conversaRepository.buscarPorId(conversa.getId())).thenReturn(Optional.of(conversa));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, conversa.getId(), "sim, confirmo"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.ACAO_EXECUTADA);
        verify(transactionServiceClient).criarTransacao(any());
        verify(transactionServiceClient, never()).criarTransacaoRecorrente(any());
        verify(llmProvider, never()).chat(any());
    }

    @Test
    void deveriaConfirmarEExecutarAcaoRecorrente_quandoUsuarioConfirma() {
        UUID contaId = UUID.randomUUID();
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.proporAcao(br.com.wepdev.financas.ai.domain.AcaoPendente.propor(
                br.com.wepdev.financas.ai.domain.TipoTransacao.RECEITA, "Salário", new BigDecimal("10000.00"),
                true, br.com.wepdev.financas.ai.domain.FrequenciaRecorrencia.MENSAL, null, contaId, "Salário"));
        when(conversaRepository.buscarPorId(conversa.getId())).thenReturn(Optional.of(conversa));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, conversa.getId(), "confirmar"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.ACAO_EXECUTADA);
        verify(transactionServiceClient).criarTransacaoRecorrente(any());
        verify(transactionServiceClient, never()).criarTransacao(any());
    }

    @Test
    void deveriaResponderExpirada_quandoConfirmaAcaoJaExpirada() {
        UUID contaId = UUID.randomUUID();
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.proporAcao(br.com.wepdev.financas.ai.domain.AcaoPendente.reconstituir(
                br.com.wepdev.financas.ai.domain.TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"), false,
                null, null, contaId, "Alimentação",
                java.time.Instant.now().minusSeconds(3600), java.time.Instant.now().minusSeconds(1800)));
        when(conversaRepository.buscarPorId(conversa.getId())).thenReturn(Optional.of(conversa));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, conversa.getId(), "confirmar"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
        assertThat(resultado.resposta()).containsIgnoringCase("expirou");
        assertThat(conversa.getAcaoPendente()).isNull();
        verify(transactionServiceClient, never()).criarTransacao(any());
        verify(transactionServiceClient, never()).criarTransacaoRecorrente(any());
        verify(llmProvider, never()).chat(any());
    }

    @Test
    void deveriaLimparAcaoPendente_quandoMensagemNaoEhConfirmacao() {
        UUID contaId = UUID.randomUUID();
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.proporAcao(br.com.wepdev.financas.ai.domain.AcaoPendente.propor(
                br.com.wepdev.financas.ai.domain.TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"),
                false, null, null, contaId, "Alimentação"));
        when(conversaRepository.buscarPorId(conversa.getId())).thenReturn(Optional.of(conversa));
        when(llmProvider.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"buscar_saldo_disponivel\", \"periodo\": null}"));
        when(budgetServiceClient.buscarDisponivelParaGastar(any())).thenReturn(
                new DisponivelParaGastar(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        ChatResultado resultado = useCase.executar(new ChatComando(usuarioId, conversa.getId(), "na verdade, quanto tenho disponível?"));

        assertThat(resultado.tipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
        assertThat(conversa.getAcaoPendente()).isNull();
        verify(transactionServiceClient, never()).criarTransacao(any());
    }

    @Test
    void deveriaLancarExcecao_quandoConversaNaoEncontrada() {
        UUID conversaId = UUID.randomUUID();
        when(conversaRepository.buscarPorId(conversaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(new ChatComando(usuarioId, conversaId, "oi")))
                .isInstanceOf(ConversaNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoConversaPertenceAOutroUsuario() {
        Conversa conversaDeOutroUsuario = Conversa.iniciar(UUID.randomUUID());
        when(conversaRepository.buscarPorId(conversaDeOutroUsuario.getId())).thenReturn(Optional.of(conversaDeOutroUsuario));

        assertThatThrownBy(() -> useCase.executar(new ChatComando(usuarioId, conversaDeOutroUsuario.getId(), "oi")))
                .isInstanceOf(ConversaNaoEncontradaException.class);
    }
}
