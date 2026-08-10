package br.com.wepdev.financas.document.manual;

import br.com.wepdev.financas.document.application.AgenteExtracaoFaturaService;
import br.com.wepdev.financas.document.domain.ChatRequest;
import br.com.wepdev.financas.document.domain.ChatResponse;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.LlmProvider;
import br.com.wepdev.financas.document.infrastructure.llm.dto.OllamaGenerateRequestDto;
import br.com.wepdev.financas.document.infrastructure.llm.dto.OllamaGenerateResponseDto;
import br.com.wepdev.financas.document.infrastructure.llm.dto.OllamaOptionsDto;
import br.com.wepdev.financas.document.infrastructure.parsing.PdfBoxExtratorTexto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste manual/exploratório — NÃO faz parte da definição de pronto
 * (testing-strategy.md seção 4 já diz pra mockar LlmProvider nos testes
 * automatizados, LLM não é determinístico). Roda só localmente, contra um
 * PDF real que o desenvolvedor colocou em {@code test-data/} (gitignored —
 * nunca existe em CI, então esse teste sempre se auto-pula lá) e contra um
 * Ollama real rodando em localhost:11500 (não 11434 — ver comentário em
 * docker-compose.yml sobre conflito de porta com Ollama nativo do Windows).
 * Senha vem de variável de ambiente, nunca hardcoded.
 */
class ExtracaoFaturaRealManualTest {

    private static final Path PASTA_TEST_DATA = Path.of("../../test-data");

    @Test
    void deveriaSoExtrairTexto_paraInspecao() throws IOException {
        String filtroArquivo = System.getenv("FATURA_TESTE_ARQUIVO");
        Assumptions.assumeTrue(filtroArquivo != null && !filtroArquivo.isBlank(),
                "FATURA_TESTE_ARQUIVO não configurada — pulando teste manual (test-data/ tem PDFs de bancos diferentes, alguns com senha; escolha explícita evita pegar o errado sem querer)");
        Optional<Path> pdf = primeiroPdfEm(PASTA_TEST_DATA, filtroArquivo);
        Assumptions.assumeTrue(pdf.isPresent(), "Nenhum PDF em test-data/ combina com FATURA_TESTE_ARQUIVO — pulando teste manual");

        String senha = System.getenv("FATURA_TESTE_SENHA");
        byte[] conteudo = Files.readAllBytes(pdf.get());
        String texto = new PdfBoxExtratorTexto().extrairTexto(conteudo, senha);

        Path saida = Path.of("target/fatura-texto-extraido.txt");
        Files.writeString(saida, texto);
        System.out.println("Texto extraído (" + texto.length() + " caracteres) salvo em " + saida.toAbsolutePath());
    }

    @Test
    void deveriaExtrairLancamentosDoDependenteNaFaturaReal() throws IOException {
        String filtroArquivo = System.getenv("FATURA_TESTE_ARQUIVO");
        Assumptions.assumeTrue(filtroArquivo != null && !filtroArquivo.isBlank(),
                "FATURA_TESTE_ARQUIVO não configurada — pulando teste manual (test-data/ tem PDFs de bancos diferentes, alguns com senha; escolha explícita evita pegar o errado sem querer)");
        Optional<Path> pdf = primeiroPdfEm(PASTA_TEST_DATA, filtroArquivo);
        Assumptions.assumeTrue(pdf.isPresent(), "Nenhum PDF em test-data/ combina com FATURA_TESTE_ARQUIVO — pulando teste manual");

        String senha = System.getenv("FATURA_TESTE_SENHA");
        String nomeFiltro = System.getenv("FATURA_TESTE_NOME_FILTRO");

        byte[] conteudo = Files.readAllBytes(pdf.get());

        LlmProvider llmProvider = construirLlmProviderOllama();
        var agente = new AgenteExtracaoFaturaService(new PdfBoxExtratorTexto(), llmProvider, new ObjectMapper());

        List<LancamentoPendente> lancamentos = agente.extrair(UUID.randomUUID(), conteudo, senha, nomeFiltro);

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Total de lançamentos extraídos: ").append(lancamentos.size()).append("\n\n");
        for (LancamentoPendente l : lancamentos) {
            relatorio.append(l.getData()).append(" | ")
                    .append(l.getTipo()).append(" | ")
                    .append(l.getValor()).append(" | ")
                    .append(l.getCategoriaSugerida()).append(" | ")
                    .append(l.getDescricao()).append("\n");
        }
        Path saida = Path.of("target/fatura-lancamentos-extraidos.txt");
        Files.writeString(saida, relatorio.toString());
        System.out.println(relatorio);
        System.out.println("Relatório salvo em " + saida.toAbsolutePath());

        assertThat(lancamentos).isNotEmpty();
    }

    /**
     * Chama o Ollama via HttpClient puro (não o OllamaRestClient real, que
     * depende do container CDI do Quarkus pra resolver — indisponível num
     * teste JUnit "pelado" como este). Mesmos DTOs/contrato JSON do
     * OllamaLlmProvider de produção, só o transporte HTTP é diferente.
     */
    private LlmProvider construirLlmProviderOllama() {
        String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11500");
        String modelo = System.getenv().getOrDefault("OLLAMA_MODEL", "llama3.1:latest");
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        ObjectMapper mapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new LlmProvider() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                try {
                    System.out.println("DEBUG prompt.length() = " + request.prompt().length());
                    String formato = request.formatoJson() ? "json" : null;
                    var corpo = new OllamaGenerateRequestDto(modelo, request.prompt(), false, formato,
                            new OllamaOptionsDto(0.1));
                    byte[] corpoBytes = mapper.writeValueAsBytes(corpo);
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + "/api/generate"))
                            .timeout(Duration.ofSeconds(900))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(corpoBytes))
                            .build();
                    HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
                    if (httpResponse.statusCode() != 200) {
                        throw new RuntimeException("Ollama respondeu " + httpResponse.statusCode() + ": "
                                + new String(httpResponse.body()));
                    }
                    var resposta = mapper.readValue(httpResponse.body(), OllamaGenerateResponseDto.class);
                    return new ChatResponse(resposta.response());
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };
    }

    /** {@code filtroNome} nulo/vazio = primeiro PDF encontrado; senão, primeiro PDF cujo nome contém o filtro (case-insensitive). */
    private Optional<Path> primeiroPdfEm(Path pasta, String filtroNome) throws IOException {
        if (!Files.isDirectory(pasta)) {
            return Optional.empty();
        }
        String filtro = filtroNome == null ? "" : filtroNome.toLowerCase();
        try (Stream<Path> arquivos = Files.list(pasta)) {
            Optional<Path> encontrado = arquivos.filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(filtro))
                    .sorted(Comparator.naturalOrder())
                    .findFirst();
            encontrado.ifPresent(p -> System.out.println("Usando fixture: " + p.getFileName()));
            return encontrado;
        }
    }
}
