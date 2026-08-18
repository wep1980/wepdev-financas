package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.ChatRequest;
import br.com.wepdev.financas.document.domain.ExtratorTexto;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.LlmProvider;
import br.com.wepdev.financas.document.infrastructure.parsing.dto.FaturaExtraidaDto;
import br.com.wepdev.financas.document.infrastructure.parsing.dto.LancamentoExtraidoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Agente de parsing de documento" citado em
 * {@code docs/architecture/ai-strategy.md} seção 4 — vive aqui porque
 * {@code ai-service} ainda não existe, mas usa a mesma porta
 * {@link LlmProvider}. Extrai o texto do PDF (ExtratorTexto), monta um
 * prompt pedindo os lançamentos em JSON, e converte o resultado em
 * {@link LancamentoPendente}. Best-effort: LLM não é determinístico
 * (testing-strategy.md seção 4) — item individual mal-formado é descartado
 * em vez de derrubar a extração inteira; resposta que não é JSON válido
 * vira lista vazia (o caso de uso que chama isso decide o que fazer com
 * "nada encontrado").
 *
 * <p>Testado na prática (2026-08-09) contra uma fatura real de várias
 * páginas com titular + dependentes: um prompt de ~14KB pedindo pro próprio
 * LLM (llama3.1 8B local) achar a seção certa e montar o JSON não funcionou
 * de forma confiável — o modelo às vezes inventava um esquema JSON
 * diferente do pedido. Por isso {@link #recortarSecaoDoNome} e
 * {@link #detectarAnoReferencia} fazem em código Java, de forma
 * determinística, o que dava pra empurrar pro LLM mas não devia: achar a
 * seção da pessoa certa (por marcador de cabeçalho, ex: "JOAO P SANTOS -
 * 4000 XXXX XXXX 0002") e o ano da fatura (linha "Vencimento"). Isso reduz
 * drasticamente o tamanho do prompt e a chance de erro — o LLM só precisa
 * extrair os lançamentos de um texto já filtrado, tarefa mais tratável pro
 * tamanho de modelo que roda local em CPU.
 */
@ApplicationScoped
public class AgenteExtracaoFaturaService {

    private static final Logger LOG = Logger.getLogger(AgenteExtracaoFaturaService.class);

    /** Ex: "JOAO P SANTOS -  4000 XXXX XXXX 0002" ou "@ MARIA C SOUZA -  4000 XXXX XXXX 0001". */
    private static final Pattern CABECALHO_SECAO_CARTAO =
            Pattern.compile("^@?\\s*([\\p{Lu}][\\p{Lu}\\s.]*?)\\s+-\\s+\\d{4}\\s+XXXX\\s+XXXX\\s+\\d{4}\\s*$");

    // Bancos escrevem a data de vencimento de jeitos diferentes: "06/08/2026"
    // (Santander/Itaú) ou "20 ABR 2026" (Nubank, mês abreviado em português).
    private static final Pattern LINHA_VENCIMENTO_NUMERICA = Pattern.compile("[Vv]encimento\\D{0,20}\\d{2}/\\d{2}/(\\d{4})");
    private static final Pattern LINHA_VENCIMENTO_MES_ABREVIADO = Pattern.compile("[Vv]encimento\\D{0,20}\\d{1,2}\\s+[A-Za-zÀ-ÿ]{3}\\.?\\s+(\\d{4})");

    private final ExtratorTexto extratorTexto;
    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;

    public AgenteExtracaoFaturaService(ExtratorTexto extratorTexto, LlmProvider llmProvider, ObjectMapper objectMapper) {
        this.extratorTexto = extratorTexto;
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * {@code senha}: nula se o PDF não for protegido (comum em fatura de
     * banco/cartão brasileira usar CPF do titular como senha).
     * {@code nomeFiltro}: nome completo do usuário autenticado (claim "name"
     * do Keycloak, ver {@code DocumentoResource.nomeUsuarioAutenticado} —
     * nunca um campo digitado no upload, 2026-08-11), nulo extrai a fatura
     * inteira; preenchido restringe a extração à seção de um
     * titular/dependente específico — uma fatura de cartão com cartão
     * adicional lista o uso de cada pessoa em seção separada (testado na
     * prática, 2026-08-09). Quando o recorte por cabeçalho de seção não
     * encontra nada (formato de fatura diferente do testado, ou nome do
     * Keycloak não bate com o nome impresso na fatura), cai de volta pro
     * texto inteiro + instrução no prompt pro LLM tentar filtrar ele mesmo.
     *
     * @throws br.com.wepdev.financas.document.domain.PdfIlegivelException se o PDF não tiver texto extraível.
     * @throws br.com.wepdev.financas.document.domain.PdfProtegidoPorSenhaException se o PDF estiver protegido e a senha não foi fornecida ou está incorreta.
     */
    public List<LancamentoPendente> extrair(UUID documentoId, byte[] conteudoArquivo, String senha, String nomeFiltro) {
        String textoFatura = extratorTexto.extrairTexto(conteudoArquivo, senha);

        Optional<String> secaoRecortada = recortarSecaoDoNome(textoFatura, nomeFiltro);
        String textoParaPrompt = secaoRecortada.orElse(textoFatura);
        String nomeFiltroParaPrompt = secaoRecortada.isPresent() ? null : nomeFiltro;

        String anoDetectado = detectarAnoReferencia(textoFatura);
        if (anoDetectado != null) {
            textoParaPrompt = "Ano de referência da fatura: " + anoDetectado + "\n\n" + textoParaPrompt;
        }

        var resposta = llmProvider.chat(ChatRequest.pedindoJson(montarPrompt(textoParaPrompt, nomeFiltroParaPrompt)));
        return parsearLancamentos(documentoId, resposta.conteudo());
    }

    /**
     * Percorre o texto linha a linha; cada linha que bate com
     * {@link #CABECALHO_SECAO_CARTAO} liga/desliga a captura conforme o
     * nome do cabeçalho contém (ou não) o primeiro E o último "nome" do
     * filtro — exige os dois pra evitar falso positivo com sobrenome
     * compartilhado entre titular e dependente (comum em fatura de família).
     * Para de capturar de vez em "Resumo da Fatura" (fim do detalhamento
     * por pessoa). Vazio se o filtro for nulo ou nenhuma seção bater.
     */
    private Optional<String> recortarSecaoDoNome(String texto, String nomeFiltro) {
        if (nomeFiltro == null || nomeFiltro.isBlank()) {
            return Optional.empty();
        }
        String[] palavrasFiltro = nomeFiltro.trim().toUpperCase().split("\\s+");
        String primeiraPalavra = palavrasFiltro[0];
        String ultimaPalavra = palavrasFiltro[palavrasFiltro.length - 1];

        StringBuilder recorte = new StringBuilder();
        boolean capturando = false;
        boolean encontrouSecao = false;
        for (String linha : texto.split("\n", -1)) {
            if (linha.contains("Resumo da Fatura")) {
                break;
            }
            Matcher m = CABECALHO_SECAO_CARTAO.matcher(linha.trim());
            if (m.matches()) {
                String nomeSecao = m.group(1).trim();
                capturando = nomeSecao.contains(primeiraPalavra) && nomeSecao.contains(ultimaPalavra);
                encontrouSecao = encontrouSecao || capturando;
            }
            if (capturando) {
                recorte.append(linha).append('\n');
            }
        }
        return encontrouSecao ? Optional.of(recorte.toString()) : Optional.empty();
    }

    private String detectarAnoReferencia(String texto) {
        Matcher numerica = LINHA_VENCIMENTO_NUMERICA.matcher(texto);
        if (numerica.find()) {
            return numerica.group(1);
        }
        Matcher mesAbreviado = LINHA_VENCIMENTO_MES_ABREVIADO.matcher(texto);
        return mesAbreviado.find() ? mesAbreviado.group(1) : null;
    }

    private String montarPrompt(String textoFatura, String nomeFiltro) {
        String instrucaoFiltro = (nomeFiltro == null || nomeFiltro.isBlank())
                ? ""
                : """
                Esta fatura pode conter o uso de mais de uma pessoa (titular \
                e dependentes, cada um com seu próprio cartão adicional, em \
                seções separadas do texto). Extraia SOMENTE os lançamentos \
                da seção referente a "%s" — ignore lançamentos de qualquer \
                outra seção/nome.

                """.formatted(nomeFiltro);

        return """
                Você é um assistente que extrai lançamentos de uma fatura de \
                cartão de crédito brasileira a partir do texto extraído do PDF.

                %sResponda APENAS com um objeto JSON, sem nenhum texto antes \
                ou depois, no formato exato:
                {
                  "anoReferencia": "AAAA",
                  "lancamentos": [
                    {"descricao": "...", "valor": "123.45", "dataTexto": "DD/MM", \
                "tipo": "DESPESA", "categoriaSugerida": "...", "numeroParcela": 1, \
                "quantidadeParcelas": 1}
                  ]
                }

                Regras:
                - "anoReferencia" é o ano da fatura (4 dígitos), geralmente \
                perto da data de vencimento no início do texto.
                - "dataTexto" é a data do lançamento EXATAMENTE como aparece \
                no texto original (não reformate, não invente o ano).
                - "valor" é sempre positivo, como texto, com ponto como \
                separador decimal, sem "R$" e sem separador de milhar.
                - "tipo" é "DESPESA" para compras normais, "RECEITA" para \
                estornos ou créditos na fatura.
                - "categoriaSugerida" é uma categoria curta em português (ex: \
                "Alimentação", "Transporte", "Assinaturas") baseada na \
                descrição — se não tiver certeza, use null.
                - "numeroParcela"/"quantidadeParcelas": se a descrição \
                mencionar parcelamento (ex: "Parcela 3/12", "3 de 12"), \
                extraia os dois números; senão, os dois valem 1 (compra à \
                vista). Nunca deixe nulo.
                - "lancamentos" é sempre uma lista, mesmo que tenha só um item.
                - Ignore linhas de total, subtotal, juros, IOF, taxa de câmbio \
                e resumo — só lançamentos individuais.
                - Se não conseguir identificar nenhum lançamento, \
                "lancamentos" deve ser uma lista vazia.

                Texto da fatura:
                %s
                """.formatted(instrucaoFiltro, textoFatura);
    }

    private List<LancamentoPendente> parsearLancamentos(UUID documentoId, String respostaJson) {
        FaturaExtraidaDto extraida;
        try {
            extraida = objectMapper.readValue(respostaJson, FaturaExtraidaDto.class);
        } catch (JsonProcessingException e) {
            LOG.warn("Resposta do LLM não é um JSON válido — tratando como nenhum lançamento encontrado");
            return List.of();
        }
        if (extraida.lancamentos() == null) {
            return List.of();
        }

        List<LancamentoPendente> lancamentos = new ArrayList<>();
        for (LancamentoExtraidoDto extraido : extraida.lancamentos()) {
            try {
                lancamentos.add(extraido.paraDominio(documentoId, extraida.anoReferencia()));
            } catch (RuntimeException e) {
                LOG.warn("Lançamento extraído em formato inesperado, descartado (best-effort)");
            }
        }
        return lancamentos;
    }
}
