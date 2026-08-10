package br.com.wepdev.financas.document.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoImportadoTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final byte[] conteudo = "conteudo-pdf-fake".getBytes();

    @Test
    void deveriaReceberComoRecebidoSemLancamento() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura-agosto.pdf", conteudo);

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.RECEBIDO);
        assertThat(documento.getLancamentos()).isEmpty();
        assertThat(documento.getId()).isNotNull();
    }

    @Test
    void deveriaLancarExcecao_quandoConteudoArquivoVazio() {
        assertThatThrownBy(() -> DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, "fatura.pdf",
                new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaIniciarProcessamento_eSerIdempotente() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura.pdf", conteudo);

        documento.iniciarProcessamento();
        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.PROCESSANDO);

        documento.iniciarProcessamento();
        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.PROCESSANDO);
    }

    @Test
    void deveriaConcluirComLancamentos_eFicarAguardandoConfirmacao() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura.pdf", conteudo);
        LancamentoPendente lancamento = LancamentoPendente.extrair(documento.getId(), "Mercado",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Alimentação");

        documento.concluirComLancamentos(List.of(lancamento));

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.AGUARDANDO_CONFIRMACAO);
        assertThat(documento.getLancamentos()).hasSize(1);
        assertThat(documento.getProcessadoEm()).isNotNull();
    }

    @Test
    void deveriaLancarExcecao_quandoConcluirComListaVazia() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura.pdf", conteudo);

        assertThatThrownBy(() -> documento.concluirComLancamentos(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaMarcarErro() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura.pdf", conteudo);

        documento.marcarErro("PDF ilegível");

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.ERRO_PROCESSAMENTO);
        assertThat(documento.getMensagemErro()).isEqualTo("PDF ilegível");
        assertThat(documento.getProcessadoEm()).isNotNull();
    }

    @Test
    void deveriaConfirmarLancamentosSelecionados_eRejeitarOsDemais() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura.pdf", conteudo);
        LancamentoPendente confirmado = LancamentoPendente.extrair(documento.getId(), "Mercado",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Alimentação");
        LancamentoPendente rejeitado = LancamentoPendente.extrair(documento.getId(), "Assinatura duvidosa",
                new BigDecimal("50.00"), LocalDate.of(2026, 8, 6), TipoLancamento.DESPESA, null);
        documento.concluirComLancamentos(List.of(confirmado, rejeitado));

        documento.confirmar(Set.of(confirmado.getId()));

        assertThat(documento.isConfirmado()).isTrue();
        assertThat(confirmado.getStatus()).isEqualTo(StatusLancamento.CONFIRMADO);
        assertThat(rejeitado.getStatus()).isEqualTo(StatusLancamento.REJEITADO);
        assertThat(documento.getLancamentosConfirmados()).containsExactly(confirmado);
    }

    @Test
    void deveriaSerIdempotente_aoConfirmarDocumentoJaConfirmado() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura.pdf", conteudo);
        LancamentoPendente lancamento = LancamentoPendente.extrair(documento.getId(), "Mercado",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Alimentação");
        documento.concluirComLancamentos(List.of(lancamento));
        documento.confirmar(Set.of(lancamento.getId()));

        documento.confirmar(Set.of());

        assertThat(documento.isConfirmado()).isTrue();
        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.CONFIRMADO);
    }

    @Test
    void naoDeveriaConfirmar_quandoAindaNaoAguardandoConfirmacao() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                "fatura.pdf", conteudo);

        assertThatThrownBy(() -> documento.confirmar(Set.of()))
                .isInstanceOf(DocumentoAindaNaoProcessadoException.class);
    }
}
