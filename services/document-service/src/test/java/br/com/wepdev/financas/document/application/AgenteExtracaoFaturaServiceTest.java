package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.ChatRequest;
import br.com.wepdev.financas.document.domain.ChatResponse;
import br.com.wepdev.financas.document.domain.ExtratorTexto;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.LlmProvider;
import br.com.wepdev.financas.document.domain.PdfIlegivelException;
import br.com.wepdev.financas.document.domain.TipoLancamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgenteExtracaoFaturaServiceTest {

    private final ExtratorTexto extratorTexto = mock(ExtratorTexto.class);
    private final LlmProvider llmProvider = mock(LlmProvider.class);
    private final AgenteExtracaoFaturaService agente =
            new AgenteExtracaoFaturaService(extratorTexto, llmProvider, new ObjectMapper());

    private final UUID documentoId = UUID.randomUUID();

    @Test
    void deveriaExtrairLancamentos_daRespostaJsonDoLlm() {
        when(extratorTexto.extrairTexto(any(), any())).thenReturn("texto da fatura extraído do pdf");
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("""
                {
                  "anoReferencia": "2026",
                  "lancamentos": [
                    {"descricao": "Supermercado", "valor": "150.00", "dataTexto": "05/08", "tipo": "DESPESA", "categoriaSugerida": "Alimentação"},
                    {"descricao": "Estorno", "valor": "-30,00", "dataTexto": "06/08/2026", "tipo": "RECEITA", "categoriaSugerida": null}
                  ]
                }
                """));

        List<LancamentoPendente> lancamentos = agente.extrair(documentoId, "pdf-fake".getBytes(), null, null);

        assertThat(lancamentos).hasSize(2);
        assertThat(lancamentos.get(0).getDescricao()).isEqualTo("Supermercado");
        assertThat(lancamentos.get(0).getValor()).isEqualByComparingTo("150.00");
        // "dataTexto" sem ano completado com anoReferencia
        assertThat(lancamentos.get(0).getData()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(lancamentos.get(0).getTipo()).isEqualTo(TipoLancamento.DESPESA);
        assertThat(lancamentos.get(0).getCategoriaSugerida()).isEqualTo("Alimentação");
        // valor negativo com vírgula decimal também precisa funcionar (LLM não é 100% consistente, testado na prática)
        assertThat(lancamentos.get(1).getValor()).isEqualByComparingTo("30.00");
        // "dataTexto" já com ano embutido também é aceita
        assertThat(lancamentos.get(1).getData()).isEqualTo(LocalDate.of(2026, 8, 6));
        assertThat(lancamentos.get(1).getCategoriaSugerida()).isNull();
    }

    @Test
    void deveriaExtrairData_noFormatoDiaMesAbreviado_eDetectarAnoDeVencimentoComMesPorExtenso() {
        // Nubank escreve data como "10 JUN" e vencimento como "17 JUL 2026" —
        // formato bem diferente do "DD/MM" do Santander/Itaú, achado real (2026-08-09).
        when(extratorTexto.extrairTexto(any(), any())).thenReturn("""
                Data de vencimento: 17 JUL 2026
                10 JUN Mercado R$ 50,00
                """);
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("""
                {
                  "anoReferencia": "2026",
                  "lancamentos": [
                    {"descricao": "Mercado", "valor": "50.00", "dataTexto": "10 JUN", "tipo": "DESPESA", "categoriaSugerida": null}
                  ]
                }
                """));

        List<LancamentoPendente> lancamentos = agente.extrair(documentoId, "pdf-fake".getBytes(), null, null);

        assertThat(lancamentos).hasSize(1);
        assertThat(lancamentos.get(0).getData()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void deveriaChamarLlmProvider_pedindoJson_comOTextoExtraidoNoPrompt() {
        when(extratorTexto.extrairTexto(any(), any())).thenReturn("CONTEUDO-UNICO-DA-FATURA");
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("""
                {"anoReferencia": "2026", "lancamentos": []}
                """));

        agente.extrair(documentoId, "pdf-fake".getBytes(), null, null);

        var captor = org.mockito.ArgumentCaptor.forClass(ChatRequest.class);
        verify(llmProvider).chat(captor.capture());
        assertThat(captor.getValue().formatoJson()).isTrue();
        assertThat(captor.getValue().prompt()).contains("CONTEUDO-UNICO-DA-FATURA");
    }

    @Test
    void deveriaRetornarListaVazia_quandoLlmNaoRetornaJsonValido() {
        when(extratorTexto.extrairTexto(any(), any())).thenReturn("texto");
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("desculpe, não consegui processar essa fatura"));

        List<LancamentoPendente> lancamentos = agente.extrair(documentoId, "pdf-fake".getBytes(), null, null);

        assertThat(lancamentos).isEmpty();
    }

    @Test
    void deveriaDescartarItemMalFormado_eManterOsDemais_quandoLlmMisturaFormatoValidoEInvalido() {
        when(extratorTexto.extrairTexto(any(), any())).thenReturn("texto");
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("""
                {
                  "anoReferencia": "2026",
                  "lancamentos": [
                    {"descricao": "Válido", "valor": "10.00", "dataTexto": "05/08", "tipo": "DESPESA", "categoriaSugerida": null},
                    {"descricao": "Sem data", "valor": "10.00", "dataTexto": "não-é-uma-data", "tipo": "DESPESA", "categoriaSugerida": null},
                    {"descricao": "Tipo inválido", "valor": "10.00", "dataTexto": "05/08", "tipo": "ALGO_ESQUISITO", "categoriaSugerida": null}
                  ]
                }
                """));

        List<LancamentoPendente> lancamentos = agente.extrair(documentoId, "pdf-fake".getBytes(), null, null);

        assertThat(lancamentos).hasSize(1);
        assertThat(lancamentos.get(0).getDescricao()).isEqualTo("Válido");
    }

    @Test
    void deveriaRecortarSoASecaoDoNomeFiltrado_eDetectarAnoDoVencimento_quandoFaturaTemMaisDeUmaPessoa() {
        when(extratorTexto.extrairTexto(any(), any())).thenReturn("""
                Fatura teste - Vencimento 06/08/2026

                MARIA C SOUZA -  4000 XXXX XXXX 0001
                Despesas
                Compra Data Descrição Parcela R$ US$
                1 05/07 LOJA DA MARIA 50,00
                VALOR TOTAL 50,00 0,00
                JOAO P SANTOS -  4000 XXXX XXXX 0002
                Despesas
                Compra Data Descrição Parcela R$ US$
                1 05/07 LOJA DO JOAO 30,00
                VALOR TOTAL 30,00 0,00
                Resumo da Fatura
                Saldo Desta Fatura 80,00
                """);
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("""
                {"anoReferencia": "2026", "lancamentos": []}
                """));

        // nome como o usuário digitaria (por extenso), documento abrevia o nome do meio — só primeiro+último nome precisam bater
        agente.extrair(documentoId, "pdf-fake".getBytes(), null, "JOAO PAULO SANTOS");

        var captor = org.mockito.ArgumentCaptor.forClass(ChatRequest.class);
        verify(llmProvider).chat(captor.capture());
        String prompt = captor.getValue().prompt();
        assertThat(prompt).contains("LOJA DO JOAO");
        assertThat(prompt).doesNotContain("LOJA DA MARIA");
        assertThat(prompt).contains("Ano de referência da fatura: 2026");
    }

    @Test
    void deveriaCairDeVoltaPraTextoInteiro_quandoNenhumaSecaoBateComOFiltro() {
        when(extratorTexto.extrairTexto(any(), any())).thenReturn("""
                Fatura teste - Vencimento 06/08/2026

                MARIA C SOUZA -  4000 XXXX XXXX 0001
                Despesas
                Compra Data Descrição Parcela R$ US$
                1 05/07 LOJA DA MARIA 50,00
                VALOR TOTAL 50,00 0,00
                Resumo da Fatura
                """);
        when(llmProvider.chat(any())).thenReturn(new ChatResponse("""
                {"anoReferencia": "2026", "lancamentos": []}
                """));

        agente.extrair(documentoId, "pdf-fake".getBytes(), null, "PEDRO SILVA");

        var captor = org.mockito.ArgumentCaptor.forClass(ChatRequest.class);
        verify(llmProvider).chat(captor.capture());
        String prompt = captor.getValue().prompt();
        assertThat(prompt).contains("LOJA DA MARIA");
        assertThat(prompt).contains("PEDRO SILVA"); // instrução de filtro no prompt, já que o recorte determinístico não achou nada
    }

    @Test
    void deveriaPropagarExcecao_quandoPdfNaoTemTextoExtraivel() {
        when(extratorTexto.extrairTexto(any(), any())).thenThrow(new PdfIlegivelException());

        assertThatThrownBy(() -> agente.extrair(documentoId, "pdf-fake".getBytes(), null, null))
                .isInstanceOf(PdfIlegivelException.class);

        verify(llmProvider, org.mockito.Mockito.never()).chat(any());
    }
}
