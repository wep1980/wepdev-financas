package br.com.wepdev.financas.document.domain;

/**
 * Porta de saída (Dependency Inversion) — extrai texto bruto de um
 * documento (PDF nessa fatia, ver ADR-0023/Apache PDFBox). Domínio e
 * aplicação não sabem qual biblioteca faz isso.
 */
public interface ExtratorTexto {

    /**
     * {@code senha} nula = PDF sem proteção. Muitas faturas de banco/cartão
     * brasileiras são protegidas por senha (ex: CPF do titular) — testado
     * na prática (2026-08-09) com uma fatura real.
     *
     * @throws PdfIlegivelException se o arquivo estiver corrompido ou não tiver camada de texto (ex: PDF escaneado como imagem).
     * @throws PdfProtegidoPorSenhaException se o PDF estiver criptografado e a senha não foi fornecida ou está incorreta.
     */
    String extrairTexto(byte[] conteudoArquivo, String senha);
}
