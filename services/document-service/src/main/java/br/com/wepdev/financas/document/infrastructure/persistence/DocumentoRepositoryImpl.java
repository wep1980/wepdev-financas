package br.com.wepdev.financas.document.infrastructure.persistence;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O agregado {@link DocumentoImportado} vive em dois bancos — metadados +
 * conteúdo bruto no MongoDB, lançamentos no MySQL (ver overview.md). Sem
 * transação distribuída entre os dois (fora de escopo pro MVP): salvar()
 * grava primeiro o documento, depois os lançamentos: se falhar no meio, o
 * pior caso é um documento sem lançamento (mesmo estado de "ainda
 * processando"), nunca um lançamento órfão.
 */
@ApplicationScoped
public class DocumentoRepositoryImpl implements DocumentoRepository {

    private final DocumentoImportadoMongoRepository mongoRepository;
    private final LancamentoPendentePanacheRepository lancamentoRepository;

    public DocumentoRepositoryImpl(DocumentoImportadoMongoRepository mongoRepository,
                                    LancamentoPendentePanacheRepository lancamentoRepository) {
        this.mongoRepository = mongoRepository;
        this.lancamentoRepository = lancamentoRepository;
    }

    /**
     * {@code @Transactional} aqui (não no caso de uso chamador) de
     * propósito — ver ADR-0024: o processamento assíncrono do documento só
     * pode começar depois que essa transação commitar de verdade, e o
     * jeito mais simples de garantir isso é o próprio salvar() só retornar
     * depois de commitado, qualquer que seja o chamador.
     */
    @Override
    @Transactional
    public void salvar(DocumentoImportado documento) {
        DocumentoImportadoEntity entity = mongoRepository.findById(documento.getId());
        if (entity == null) {
            mongoRepository.persist(DocumentoImportadoMapper.paraNovaEntidade(documento));
        } else {
            DocumentoImportadoMapper.atualizarEntidade(entity, documento);
            mongoRepository.update(entity);
        }

        for (LancamentoPendente lancamento : documento.getLancamentos()) {
            LancamentoPendenteEntity lancamentoEntity = lancamentoRepository.findById(lancamento.getId());
            if (lancamentoEntity == null) {
                lancamentoRepository.persist(LancamentoPendenteMapper.paraNovaEntidade(lancamento));
            } else {
                LancamentoPendenteMapper.atualizarEntidade(lancamentoEntity, lancamento);
            }
        }
    }

    /**
     * {@code @Transactional} aqui também: chamado a partir da thread do
     * {@code ManagedExecutor} durante o processamento em background
     * (ProcessarDocumentoService), onde não existe request HTTP nem
     * transação ativa — sem isso, o Hibernate ORM recusa a leitura
     * (testado na prática: ContextNotActiveException).
     */
    @Override
    @Transactional
    public Optional<DocumentoImportado> buscarPorId(UUID id, UUID usuarioId) {
        DocumentoImportadoEntity entity = mongoRepository.findById(id);
        if (entity == null || !entity.usuarioId.equals(usuarioId)) {
            return Optional.empty();
        }
        return Optional.of(DocumentoImportadoMapper.paraDominio(entity, buscarLancamentos(id)));
    }

    @Override
    public List<DocumentoImportado> listarPorUsuario(UUID usuarioId, StatusDocumento statusFiltro) {
        List<DocumentoImportadoEntity> entities = statusFiltro == null
                ? mongoRepository.list("usuarioId", usuarioId)
                : mongoRepository.list("usuarioId = ?1 and status = ?2", usuarioId, statusFiltro.name());
        return entities.stream()
                .sorted((a, b) -> b.criadoEm.compareTo(a.criadoEm))
                .map(entity -> DocumentoImportadoMapper.paraDominio(entity, buscarLancamentos(entity.id)))
                .toList();
    }

    private List<LancamentoPendente> buscarLancamentos(UUID documentoId) {
        return lancamentoRepository.listarPorDocumento(documentoId).stream()
                .map(LancamentoPendenteMapper::paraDominio)
                .toList();
    }
}
