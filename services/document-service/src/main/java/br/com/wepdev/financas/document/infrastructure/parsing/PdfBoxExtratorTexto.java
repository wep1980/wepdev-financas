package br.com.wepdev.financas.document.infrastructure.parsing;

import br.com.wepdev.financas.document.domain.ExtratorTexto;
import br.com.wepdev.financas.document.domain.PdfIlegivelException;
import br.com.wepdev.financas.document.domain.PdfProtegidoPorSenhaException;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/** Implementação com Apache PDFBox (ADR-0023) — única biblioteca de extração usada nessa fatia. */
@ApplicationScoped
public class PdfBoxExtratorTexto implements ExtratorTexto {

    @Override
    public String extrairTexto(byte[] conteudoArquivo, String senha) {
        try (PDDocument documento = senha == null
                ? Loader.loadPDF(conteudoArquivo)
                : Loader.loadPDF(conteudoArquivo, senha)) {
            String texto = new PDFTextStripper().getText(documento);
            if (texto == null || texto.isBlank()) {
                throw new PdfIlegivelException();
            }
            return texto;
        } catch (InvalidPasswordException e) {
            throw new PdfProtegidoPorSenhaException();
        } catch (IOException e) {
            throw new PdfIlegivelException(e);
        }
    }
}
