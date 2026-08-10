package br.com.wepdev.financas.document.infrastructure.parsing;

import br.com.wepdev.financas.document.domain.PdfIlegivelException;
import br.com.wepdev.financas.document.domain.PdfProtegidoPorSenhaException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfBoxExtratorTextoTest {

    private final PdfBoxExtratorTexto extrator = new PdfBoxExtratorTexto();

    @Test
    void deveriaExtrairTexto_deUmPdfValido() throws IOException {
        byte[] pdf = gerarPdfComTexto("Fatura de teste - Supermercado 150.00", null);

        String texto = extrator.extrairTexto(pdf, null);

        assertThat(texto).contains("Fatura de teste");
    }

    @Test
    void deveriaLancarExcecao_quandoBytesNaoSaoUmPdfValido() {
        byte[] naoEhPdf = "isso não é um PDF".getBytes();

        assertThatThrownBy(() -> extrator.extrairTexto(naoEhPdf, null))
                .isInstanceOf(PdfIlegivelException.class);
    }

    @Test
    void deveriaExtrairTexto_deUmPdfProtegidoPorSenha_quandoSenhaCorreta() throws IOException {
        byte[] pdf = gerarPdfComTexto("Fatura protegida - Farmácia 30.00", "12345678900");

        String texto = extrator.extrairTexto(pdf, "12345678900");

        assertThat(texto).contains("Fatura protegida");
    }

    @Test
    void deveriaLancarExcecao_quandoPdfProtegidoESenhaAusenteOuIncorreta() throws IOException {
        byte[] pdf = gerarPdfComTexto("Fatura protegida - Farmácia 30.00", "12345678900");

        assertThatThrownBy(() -> extrator.extrairTexto(pdf, null))
                .isInstanceOf(PdfProtegidoPorSenhaException.class);
        assertThatThrownBy(() -> extrator.extrairTexto(pdf, "senha-errada"))
                .isInstanceOf(PdfProtegidoPorSenhaException.class);
    }

    private byte[] gerarPdfComTexto(String texto, String senha) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                conteudo.newLineAtOffset(50, 700);
                conteudo.showText(texto);
                conteudo.endText();
            }
            if (senha != null) {
                StandardProtectionPolicy protecao = new StandardProtectionPolicy("dono-" + senha, senha, new AccessPermission());
                protecao.setEncryptionKeyLength(128);
                documento.protect(protecao);
            }
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        }
    }
}
