package br.com.wepdev.financas.document.domain;

/** PDF corrompido ou sem camada de texto (ex: escaneado como imagem, precisaria de visão/OCR — fora do escopo dessa fatia, ver ADR-0015). */
public class PdfIlegivelException extends RuntimeException {

    public PdfIlegivelException() {
        super("Não foi possível extrair texto do PDF");
    }

    public PdfIlegivelException(Throwable causa) {
        super("Não foi possível extrair texto do PDF", causa);
    }
}
