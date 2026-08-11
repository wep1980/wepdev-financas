package br.com.wepdev.financas.document.infrastructure.parsing.dto;

import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato do JSON que o prompt pede ao LLM (ver AgenteExtracaoFaturaService)
 * — campos como String de propósito, LLM não é confiável pra sempre devolver
 * tipo JSON nativo correto (ex: valor entre aspas). {@code dataTexto} é
 * pedida EXATAMENTE como escrita na fatura (ex: "05/08" ou "10 JUN"), sem o
 * LLM tentar reformatar — testado na prática (2026-08-09) que pedir pro
 * modelo montar a data ISO ele mesmo errava dia/mês; completar o ano em
 * código determinístico é mais confiável que pedir aritmética de data ao
 * LLM. Bancos diferentes escrevem a data de jeitos diferentes — Santander/
 * Itaú usam "DD/MM", Nubank usa "DD MES" (mês abreviado em português, ex:
 * "10 JUN") — parsearData tenta os formatos conhecidos em sequência.
 */
public record LancamentoExtraidoDto(String descricao, String valor, String dataTexto, String tipo,
                                     String categoriaSugerida, Integer numeroParcela, Integer quantidadeParcelas) {

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern DIA_MES_ABREVIADO = Pattern.compile("(\\d{1,2})\\s+([A-Za-zÀ-ÿ]{3})\\.?");
    private static final Map<String, Integer> MESES_ABREVIADOS_PT = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("FEV", 2), Map.entry("MAR", 3), Map.entry("ABR", 4),
            Map.entry("MAI", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AGO", 8),
            Map.entry("SET", 9), Map.entry("OUT", 10), Map.entry("NOV", 11), Map.entry("DEZ", 12)
    );
    /** "Parcela 8/11", "Parcela 08 / 11" — formato usado por Nubank/Itaú/Santander, testado na prática. */
    private static final Pattern PADRAO_PARCELA = Pattern.compile("Parcela\\s+(\\d+)\\s*/\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /** @throws RuntimeException (várias subclasses) se algum campo vier em formato inesperado — quem chama decide se descarta esse item (best-effort, ver AgenteExtracaoFaturaService). */
    public LancamentoPendente paraDominio(UUID documentoId, String anoReferencia) {
        int[] parcela = resolverParcela();
        return LancamentoPendente.extrair(
                documentoId,
                descricao.trim(),
                normalizarValor(valor),
                parsearData(dataTexto, anoReferencia),
                TipoLancamento.valueOf(tipo.trim().toUpperCase()),
                (categoriaSugerida == null || categoriaSugerida.isBlank()) ? null : categoriaSugerida.trim(),
                parcela[0],
                parcela[1]
        );
    }

    /**
     * Prioriza os campos estruturados pedidos ao LLM (mais confiável — o
     * modelo já enxerga o texto inteiro e o contexto); se vierem nulos
     * (LLM às vezes omite mesmo quando pedido, ver Javadoc de
     * AgenteExtracaoFaturaService sobre confiabilidade), cai pro
     * reconhecimento determinístico de "Parcela X/Y" na descrição como
     * rede de segurança (2026-08-11, ver ADR-0028). Sem nenhum dos dois,
     * assume à vista (1/1).
     */
    private int[] resolverParcela() {
        if (numeroParcela != null && quantidadeParcelas != null) {
            return new int[]{numeroParcela, quantidadeParcelas};
        }
        Matcher parcela = PADRAO_PARCELA.matcher(descricao);
        if (parcela.find()) {
            return new int[]{Integer.parseInt(parcela.group(1)), Integer.parseInt(parcela.group(2))};
        }
        return new int[]{1, 1};
    }

    private static LocalDate parsearData(String dataTexto, String anoReferencia) {
        String limpo = dataTexto.trim();
        try {
            return LocalDate.parse(limpo, DD_MM_YYYY);
        } catch (DateTimeParseException ignorada) {
            // segue tentando os formatos abaixo
        }
        try {
            return LocalDate.parse(limpo); // já veio ISO (AAAA-MM-DD) — aceito também
        } catch (DateTimeParseException ignorada) {
            // segue tentando o formato abaixo
        }
        Matcher diaMesAbreviado = DIA_MES_ABREVIADO.matcher(limpo);
        if (diaMesAbreviado.matches()) {
            Integer mes = MESES_ABREVIADOS_PT.get(diaMesAbreviado.group(2).toUpperCase());
            if (mes != null) {
                return LocalDate.of(Integer.parseInt(anoReferencia.trim()), mes,
                        Integer.parseInt(diaMesAbreviado.group(1)));
            }
        }
        return LocalDate.parse(limpo + "/" + anoReferencia.trim(), DD_MM_YYYY);
    }

    /**
     * Sempre retorna positivo — o sinal do lançamento é expresso pelo campo
     * "tipo" (RECEITA/DESPESA), mesma convenção do transaction-service/
     * card-service. O LLM às vezes devolve valor negativo pra estorno mesmo
     * sendo instruído a não fazer isso (testado na prática) — .abs()
     * absorve isso em vez de descartar o lançamento.
     */
    private static BigDecimal normalizarValor(String bruto) {
        String limpo = bruto.replaceAll("[^0-9,.-]", "").trim();
        if (limpo.contains(",") && !limpo.contains(".")) {
            limpo = limpo.replace(",", ".");
        } else {
            limpo = limpo.replace(",", "");
        }
        return new BigDecimal(limpo).abs();
    }
}
