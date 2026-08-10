package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.PdfIlegivelException;
import br.com.wepdev.financas.document.domain.PdfProtegidoPorSenhaException;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Roda em background (disparado por {@link UploadDocumentoUseCase} via
 * {@code ManagedExecutor}, ver ADR-0024 — extração real leva minutos, não
 * dá pra ser síncrona). Cada chamada a {@code repository.salvar()} é sua
 * própria transação (ver {@code @Transactional} em
 * {@code DocumentoRepositoryImpl.salvar}) — não segura uma transação
 * aberta durante os minutos da chamada ao LLM.
 */
@ApplicationScoped
public class ProcessarDocumentoService {

    private static final Logger LOG = Logger.getLogger(ProcessarDocumentoService.class);

    private final DocumentoRepository repository;
    private final AgenteExtracaoFaturaService agente;

    public ProcessarDocumentoService(DocumentoRepository repository, AgenteExtracaoFaturaService agente) {
        this.repository = repository;
        this.agente = agente;
    }

    public void processar(UUID documentoId, UUID usuarioId, byte[] conteudoArquivo, String senha, String nomeFiltro) {
        DocumentoImportado documento = buscarOuLancar(documentoId, usuarioId);
        documento.iniciarProcessamento();
        repository.salvar(documento);

        try {
            List<LancamentoPendente> lancamentos = agente.extrair(documentoId, conteudoArquivo, senha, nomeFiltro);
            if (lancamentos.isEmpty()) {
                documento.marcarErro("Nenhum lançamento reconhecido no documento");
            } else {
                documento.concluirComLancamentos(lancamentos);
            }
        } catch (PdfIlegivelException | PdfProtegidoPorSenhaException e) {
            documento.marcarErro(e.getMessage());
        } catch (RuntimeException e) {
            // nunca logar conteudoArquivo/prompt/resposta do LLM (dado financeiro sensível, ver CLAUDE.md).
            LOG.error("Erro inesperado ao processar documento " + documentoId, e);
            documento.marcarErro("Erro inesperado ao processar documento");
        }
        repository.salvar(documento);
    }

    private DocumentoImportado buscarOuLancar(UUID documentoId, UUID usuarioId) {
        return repository.buscarPorId(documentoId, usuarioId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(documentoId));
    }
}
