package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.AcaoPendente;
import br.com.wepdev.financas.ai.domain.AcaoPendenteExpiradaException;
import br.com.wepdev.financas.ai.domain.AccountServiceClient;
import br.com.wepdev.financas.ai.domain.BudgetServiceClient;
import br.com.wepdev.financas.ai.domain.CardServiceClient;
import br.com.wepdev.financas.ai.domain.Cartao;
import br.com.wepdev.financas.ai.domain.ChatRequest;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIaRepository;
import br.com.wepdev.financas.ai.domain.Conta;
import br.com.wepdev.financas.ai.domain.Conversa;
import br.com.wepdev.financas.ai.domain.ConversaNaoEncontradaException;
import br.com.wepdev.financas.ai.domain.ConversaRepository;
import br.com.wepdev.financas.ai.domain.CriarTransacaoComando;
import br.com.wepdev.financas.ai.domain.CriarTransacaoRecorrenteComando;
import br.com.wepdev.financas.ai.domain.DisponivelParaGastar;
import br.com.wepdev.financas.ai.domain.Fatura;
import br.com.wepdev.financas.ai.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.ai.domain.Intencao;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.domain.LlmProviderFactory;
import br.com.wepdev.financas.ai.domain.PeriodoReferencia;
import br.com.wepdev.financas.ai.domain.ResultadoBusca;
import br.com.wepdev.financas.ai.domain.ResumoCategoria;
import br.com.wepdev.financas.ai.domain.StatusFatura;
import br.com.wepdev.financas.ai.domain.TipoRespostaAgente;
import br.com.wepdev.financas.ai.domain.TipoTransacao;
import br.com.wepdev.financas.ai.domain.ToolConsulta;
import br.com.wepdev.financas.ai.domain.TransactionServiceClient;
import br.com.wepdev.financas.ai.infrastructure.agent.dto.AcaoExtraidaDto;
import br.com.wepdev.financas.ai.infrastructure.agent.dto.IntencaoDetectadaDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agente orquestrador (ai-strategy.md seção 4) — único endpoint pra tudo
 * que acontece na conversa: pergunta nova, comando de ação, correção de
 * proposta pendente e confirmação, todos entram por {@link #executar}.
 * A intenção é decidida pelo texto + estado da conversa, nunca por um
 * endpoint separado.
 */
@ApplicationScoped
public class AgenteOrquestradorUseCase {

    private static final Logger LOG = Logger.getLogger(AgenteOrquestradorUseCase.class);

    /** Casamento por substring, case-insensitive — suficiente pra confirmação de ação financeira (baixo risco de falso positivo nessas frases). */
    private static final Set<String> PALAVRAS_CONFIRMACAO = Set.of(
            "sim", "confirmo", "confirma", "confirmar", "pode confirmar", "ok", "isso mesmo", "certo", "pode criar"
    );

    private static final String PROMPT_CLASSIFICACAO = """
            Você é um assistente financeiro. Classifique a mensagem do usuário abaixo.

            Se for uma pergunta sobre a situação financeira, responda:
            {"intent": "CONSULTA", "tool": "<uma das tools abaixo>", "periodo": "<um dos períodos abaixo, ou null>"}

            Tools disponíveis:
            - buscar_saldo_disponivel: quanto tem disponível pra gastar no mês
            - resumo_gastos_por_categoria: quanto gastou por categoria, comparado ao período anterior
            - buscar_fatura_cartao: valor e vencimento de fatura de cartão de crédito
            - buscar_transacoes: buscar transações específicas por descrição

            Períodos disponíveis (use quando a pergunta mencionar um período, senão null):
            MES_ATUAL, MES_PASSADO, ULTIMOS_3_MESES

            Se for um comando pra criar uma receita ou despesa (nova ou recorrente), responda:
            {"intent": "ACAO"}

            Se não for nada disso, responda:
            {"intent": "DESCONHECIDA"}

            Mensagem do usuário: "%s"

            Responda só com o JSON, sem nenhum texto antes ou depois.
            """;

    private static final String PROMPT_EXTRACAO_ACAO = """
            Extraia os parâmetros da seguinte ação financeira em JSON, no formato exato:
            {
              "tipo": "RECEITA ou DESPESA",
              "descricao": "descrição curta do que é a receita/despesa em si (NUNCA o nome da conta), ex: Salário, Aluguel, Assinatura Netflix — se não houver nada específico mencionado, use 'Despesa' ou 'Receita'",
              "valor": 123.45,
              "recorrente": true ou false,
              "quantidadeOcorrencias": número inteiro ou null (null = indefinida, só relevante se recorrente=true),
              "categoria": "categoria curta em português, ou null",
              "contaTexto": "nome da conta mencionada pelo usuário (ex: conta corrente, carteira), ou null se não mencionada — NUNCA repita esse valor em descricao"
            }

            Comando: "%s"

            Responda só com o JSON, sem nenhum texto antes ou depois.
            """;

    private final ConversaRepository conversaRepository;
    private final ConfiguracaoIaRepository configuracaoIaRepository;
    private final LlmProviderFactory llmProviderFactory;
    private final AccountServiceClient accountServiceClient;
    private final BudgetServiceClient budgetServiceClient;
    private final CardServiceClient cardServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final BuscarTransacoesSimilaresUseCase buscarTransacoesSimilaresUseCase;
    private final ObjectMapper objectMapper;

    public AgenteOrquestradorUseCase(ConversaRepository conversaRepository, ConfiguracaoIaRepository configuracaoIaRepository,
                                      LlmProviderFactory llmProviderFactory, AccountServiceClient accountServiceClient,
                                      BudgetServiceClient budgetServiceClient, CardServiceClient cardServiceClient,
                                      TransactionServiceClient transactionServiceClient,
                                      BuscarTransacoesSimilaresUseCase buscarTransacoesSimilaresUseCase, ObjectMapper objectMapper) {
        this.conversaRepository = conversaRepository;
        this.configuracaoIaRepository = configuracaoIaRepository;
        this.llmProviderFactory = llmProviderFactory;
        this.accountServiceClient = accountServiceClient;
        this.budgetServiceClient = budgetServiceClient;
        this.cardServiceClient = cardServiceClient;
        this.transactionServiceClient = transactionServiceClient;
        this.buscarTransacoesSimilaresUseCase = buscarTransacoesSimilaresUseCase;
        this.objectMapper = objectMapper;
    }

    public ChatResultado executar(ChatComando comando) {
        Conversa conversa = carregarOuIniciar(comando);
        conversa.adicionarMensagemUsuario(comando.mensagem());

        Instant agora = Instant.now();
        ChatResultado resultado;
        if (conversa.getAcaoPendente() != null && pareceConfirmacao(comando.mensagem())) {
            // Validade (expirada ou não) é decidida dentro de confirmarEExecutarAcao —
            // checar só temAcaoPendenteValida aqui excluiria a proposta expirada ANTES
            // de conseguir avisar o usuário disso, ela só cairia num "não entendi" genérico.
            resultado = confirmarEExecutarAcao(conversa, agora);
        } else {
            conversa.limparAcaoPendente();
            ConfiguracaoIa configuracaoIa = configuracaoIaRepository.buscarPorUsuario(comando.usuarioId())
                    .orElseGet(() -> ConfiguracaoIa.semDefinir(comando.usuarioId()));
            LlmProvider llmProvider = llmProviderFactory.criar(configuracaoIa);
            IntencaoDetectadaDto intencaoDetectada = classificarIntencao(llmProvider, comando.mensagem());
            resultado = despachar(intencaoDetectada, llmProvider, conversa, comando);
        }

        conversaRepository.salvar(conversa);
        return resultado;
    }

    private Conversa carregarOuIniciar(ChatComando comando) {
        if (comando.conversaId() == null) {
            return Conversa.iniciar(comando.usuarioId());
        }
        return conversaRepository.buscarPorId(comando.conversaId())
                .filter(c -> c.getUsuarioId().equals(comando.usuarioId()))
                .orElseThrow(() -> new ConversaNaoEncontradaException(comando.conversaId()));
    }

    private boolean pareceConfirmacao(String mensagem) {
        String normalizada = normalizar(mensagem);
        return PALAVRAS_CONFIRMACAO.stream().anyMatch(normalizada::contains);
    }

    private ChatResultado despachar(IntencaoDetectadaDto intencaoDetectada, LlmProvider llmProvider, Conversa conversa,
                                     ChatComando comando) {
        Intencao intencao = paraIntencao(intencaoDetectada.intent());
        return switch (intencao) {
            case CONSULTA -> responderConsulta(intencaoDetectada, conversa, comando);
            case ACAO -> proporAcao(llmProvider, conversa, comando);
            case DESCONHECIDA -> respostaSimples(conversa, "Não entendi. Pode reformular sua pergunta ou comando?", TipoRespostaAgente.RESPOSTA);
        };
    }

    // ---- Fluxo de confirmação (ADR-0007) ----

    private ChatResultado confirmarEExecutarAcao(Conversa conversa, Instant agora) {
        AcaoPendente acao;
        try {
            acao = conversa.confirmarAcaoPendente(agora);
        } catch (AcaoPendenteExpiradaException e) {
            return respostaSimples(conversa, "Essa proposta expirou. Pode pedir de novo?", TipoRespostaAgente.RESPOSTA);
        }

        List<RegistroTrace> trace = new ArrayList<>();
        if (acao.isRecorrente()) {
            transactionServiceClient.criarTransacaoRecorrente(new CriarTransacaoRecorrenteComando(
                    acao.getContaId(), acao.getDescricao(), acao.getValor(), acao.getTipo(), acao.getCategoria(),
                    acao.getFrequencia(), LocalDate.now(), acao.getQuantidadeOcorrencias()));
            trace.add(new RegistroTrace("criar_transacao", "Regra recorrente criada no transaction-service"));
        } else {
            transactionServiceClient.criarTransacao(new CriarTransacaoComando(
                    acao.getContaId(), acao.getDescricao(), acao.getValor(), acao.getTipo(), acao.getCategoria(), LocalDate.now()));
            trace.add(new RegistroTrace("criar_transacao", "Transação criada no transaction-service"));
        }

        String resposta = "Pronto! Criei " + resumoAcao(acao) + ".";
        conversa.adicionarRespostaAgente(resposta, TipoRespostaAgente.ACAO_EXECUTADA);
        return new ChatResultado(conversa.getId(), resposta, TipoRespostaAgente.ACAO_EXECUTADA, null, trace);
    }

    // ---- Fluxo de ação — extrai, resolve conta, propõe (nunca executa direto) ----

    private ChatResultado proporAcao(LlmProvider llmProvider, Conversa conversa, ChatComando comando) {
        AcaoExtraidaDto extraida = extrairAcao(llmProvider, comando.mensagem());
        if (extraida == null || extraida.tipo() == null || extraida.valor() == null || extraida.descricao() == null) {
            return respostaSimples(conversa, "Não consegui entender os detalhes dessa ação. Pode reformular, dizendo o valor e se é receita ou despesa?", TipoRespostaAgente.RESPOSTA);
        }

        TipoTransacao tipo;
        try {
            tipo = TipoTransacao.valueOf(extraida.tipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            return respostaSimples(conversa, "Não entendi se é uma receita ou despesa. Pode dizer de novo?", TipoRespostaAgente.RESPOSTA);
        }

        UUID contaId = resolverConta(extraida.contaTexto(), comando.usuarioId());
        if (contaId == null) {
            return respostaSimples(conversa, "Qual conta devo usar? (ex: conta corrente, carteira)", TipoRespostaAgente.RESPOSTA);
        }

        boolean recorrente = Boolean.TRUE.equals(extraida.recorrente());
        FrequenciaRecorrencia frequencia = recorrente ? FrequenciaRecorrencia.MENSAL : null;

        AcaoPendente acaoPendente;
        try {
            acaoPendente = AcaoPendente.propor(tipo, extraida.descricao(), extraida.valor(), recorrente, frequencia,
                    extraida.quantidadeOcorrencias(), contaId, extraida.categoria());
        } catch (RuntimeException e) {
            return respostaSimples(conversa, "Não consegui montar essa ação (valor precisa ser positivo). Pode reformular?", TipoRespostaAgente.RESPOSTA);
        }

        conversa.proporAcao(acaoPendente);
        String resposta = "Vou criar " + resumoAcao(acaoPendente) + ". Confirma?";
        conversa.adicionarRespostaAgente(resposta, TipoRespostaAgente.PROPOSTA_ACAO);
        return new ChatResultado(conversa.getId(), resposta, TipoRespostaAgente.PROPOSTA_ACAO, acaoPendente, List.of());
    }

    private UUID resolverConta(String contaTexto, UUID usuarioId) {
        if (contaTexto == null || contaTexto.isBlank()) {
            return null;
        }
        String alvo = normalizar(contaTexto);
        List<Conta> contas = accountServiceClient.buscarContasAtivas();
        return contas.stream()
                .filter(c -> normalizar(c.nome()).contains(alvo) || alvo.contains(normalizar(c.nome())))
                .map(Conta::id)
                .findFirst()
                .orElse(null);
    }

    private String resumoAcao(AcaoPendente acao) {
        String tipoLegivel = acao.getTipo() == TipoTransacao.RECEITA ? "receita" : "despesa";
        String valorFormatado = formatarValor(acao.getValor());
        if (acao.isRecorrente()) {
            String duracao = acao.getQuantidadeOcorrencias() == null
                    ? "sem data de término"
                    : "por " + acao.getQuantidadeOcorrencias() + " meses";
            return "uma " + tipoLegivel + " recorrente de R$" + valorFormatado + "/mês (" + acao.getDescricao() + "), " + duracao;
        }
        return "uma " + tipoLegivel + " de R$" + valorFormatado + " (" + acao.getDescricao() + ")";
    }

    // ---- Fluxo de consulta — chama a tool certa, monta resposta determinística (nunca deixa o LLM inventar o número) ----

    private ChatResultado responderConsulta(IntencaoDetectadaDto intencaoDetectada, Conversa conversa, ChatComando comando) {
        var tool = paraTool(intencaoDetectada.tool());
        if (tool.isEmpty()) {
            return respostaSimples(conversa, "Não entendi bem sua pergunta. Pode reformular?", TipoRespostaAgente.RESPOSTA);
        }
        PeriodoReferencia periodo = paraPeriodo(intencaoDetectada.periodo()).orElse(PeriodoReferencia.MES_ATUAL);

        List<RegistroTrace> trace = new ArrayList<>();
        String resposta = switch (tool.get()) {
            case SALDO_DISPONIVEL -> responderSaldoDisponivel(periodo, trace);
            case RESUMO_CATEGORIA -> responderResumoCategoria(periodo, trace);
            case FATURA_CARTAO -> responderFaturaCartao(trace);
            case TRANSACOES -> responderTransacoes(comando.usuarioId(), comando.mensagem(), trace);
        };

        conversa.adicionarRespostaAgente(resposta, TipoRespostaAgente.RESPOSTA);
        return new ChatResultado(conversa.getId(), resposta, TipoRespostaAgente.RESPOSTA, null, trace);
    }

    private String responderSaldoDisponivel(PeriodoReferencia periodo, List<RegistroTrace> trace) {
        DisponivelParaGastar disponivel = budgetServiceClient.buscarDisponivelParaGastar(periodo.mesReferencia());
        trace.add(new RegistroTrace("buscar_saldo_disponivel", "budget-service, mês " + periodo.mesReferencia()));
        return "Você tem R$" + formatarValor(disponivel.valorDisponivel()) + " disponível pra gastar em "
                + periodo.mesReferencia() + ".";
    }

    private String responderResumoCategoria(PeriodoReferencia periodo, List<RegistroTrace> trace) {
        List<ResumoCategoria> resumo = transactionServiceClient.buscarResumoPorCategoria(periodo.inicio(), periodo.fim());
        trace.add(new RegistroTrace("resumo_gastos_por_categoria", "transaction-service, " + periodo.inicio() + " a " + periodo.fim()));
        if (resumo.isEmpty()) {
            return "Não encontrei gastos nesse período.";
        }
        String detalhe = resumo.stream()
                .map(r -> r.categoria() + ": R$" + formatarValor(r.totalGasto()))
                .collect(Collectors.joining(", "));

        // transaction-service já calcula o período anterior de mesma duração por categoria
        // (ResumoCategoriaResponse.totalGastoPeriodoAnterior) — soma aqui pra responder
        // direto perguntas tipo "gastei mais ou menos que o mês passado" sem precisar
        // pedir aritmética ao LLM (mesma lição do document-service: aritmética em Java, não no prompt).
        boolean temComparacao = resumo.stream().allMatch(r -> r.totalGastoPeriodoAnterior() != null);
        String comparacao = "";
        if (temComparacao) {
            BigDecimal totalAtual = resumo.stream().map(ResumoCategoria::totalGasto).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalAnterior = resumo.stream().map(ResumoCategoria::totalGastoPeriodoAnterior).reduce(BigDecimal.ZERO, BigDecimal::add);
            int comparacaoResultado = totalAtual.compareTo(totalAnterior);
            String tendencia = comparacaoResultado > 0 ? "maior" : comparacaoResultado < 0 ? "menor" : "igual";
            comparacao = " No total, R$" + formatarValor(totalAtual) + ", " + tendencia
                    + " que o período anterior de mesma duração (R$" + formatarValor(totalAnterior) + ").";
        }

        return "Seus gastos por categoria: " + detalhe + "." + comparacao;
    }

    private String responderFaturaCartao(List<RegistroTrace> trace) {
        List<Cartao> cartoes = cardServiceClient.buscarCartoesAtivos();
        trace.add(new RegistroTrace("buscar_fatura_cartao", "card-service"));
        if (cartoes.isEmpty()) {
            return "Você não tem cartão cadastrado.";
        }
        // v1: primeiro cartão ativo — resolver "cartão X" por nome fica pra uma melhoria futura (ver docs/tasks.md item 8).
        Cartao cartao = cartoes.get(0);
        List<Fatura> faturas = cardServiceClient.buscarFaturas(cartao.id(), StatusFatura.FECHADA);
        if (faturas.isEmpty()) {
            return "Não encontrei fatura fechada do cartão " + cartao.apelido() + ".";
        }
        Fatura fatura = faturas.get(0);
        return "A fatura do cartão " + cartao.apelido() + " é de R$" + formatarValor(fatura.valorTotal())
                + ", vence em " + fatura.dataVencimento() + ".";
    }

    private String responderTransacoes(UUID usuarioId, String mensagemOriginal, List<RegistroTrace> trace) {
        List<ResultadoBusca> resultados = buscarTransacoesSimilaresUseCase.executar(usuarioId, mensagemOriginal, 5);
        trace.add(new RegistroTrace("buscar_transacoes", "busca semântica no Qdrant"));
        if (resultados.isEmpty()) {
            return "Não encontrei transações parecidas com isso.";
        }
        String lista = resultados.stream().map(ResultadoBusca::texto).collect(Collectors.joining(", "));
        return "Encontrei: " + lista + ".";
    }

    // ---- Chamadas ao LLM (JSON estruturado, best-effort — mesmo padrão do document-service/AgenteExtracaoFaturaService) ----

    private IntencaoDetectadaDto classificarIntencao(LlmProvider llmProvider, String mensagem) {
        var resposta = llmProvider.chat(ChatRequest.pedindoJson(PROMPT_CLASSIFICACAO.formatted(mensagem)));
        try {
            return objectMapper.readValue(resposta.conteudo(), IntencaoDetectadaDto.class);
        } catch (JsonProcessingException e) {
            LOG.warn("Resposta do LLM não é JSON válido na classificação de intenção — tratando como DESCONHECIDA");
            return new IntencaoDetectadaDto("DESCONHECIDA", null, null);
        }
    }

    private AcaoExtraidaDto extrairAcao(LlmProvider llmProvider, String mensagem) {
        var resposta = llmProvider.chat(ChatRequest.pedindoJson(PROMPT_EXTRACAO_ACAO.formatted(mensagem)));
        try {
            return objectMapper.readValue(resposta.conteudo(), AcaoExtraidaDto.class);
        } catch (JsonProcessingException e) {
            LOG.warn("Resposta do LLM não é JSON válido na extração de parâmetros de ação");
            return null;
        }
    }

    // ---- Utilitários ----

    private ChatResultado respostaSimples(Conversa conversa, String texto, TipoRespostaAgente tipo) {
        conversa.adicionarRespostaAgente(texto, tipo);
        return new ChatResultado(conversa.getId(), texto, tipo, null, List.of());
    }

    private Intencao paraIntencao(String valor) {
        try {
            return valor == null ? Intencao.DESCONHECIDA : Intencao.valueOf(valor.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Intencao.DESCONHECIDA;
        }
    }

    private Optional<ToolConsulta> paraTool(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return switch (valor.toLowerCase()) {
            case "buscar_saldo_disponivel" -> Optional.of(ToolConsulta.SALDO_DISPONIVEL);
            case "resumo_gastos_por_categoria" -> Optional.of(ToolConsulta.RESUMO_CATEGORIA);
            case "buscar_fatura_cartao" -> Optional.of(ToolConsulta.FATURA_CARTAO);
            case "buscar_transacoes" -> Optional.of(ToolConsulta.TRANSACOES);
            default -> Optional.empty();
        };
    }

    private Optional<PeriodoReferencia> paraPeriodo(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(PeriodoReferencia.valueOf(valor.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String normalizar(String texto) {
        return texto.toLowerCase().trim();
    }

    private String formatarValor(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
