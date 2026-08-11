package br.com.wepdev.financas.document.infrastructure.persistence;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

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
 *
 * <p>Achado real (2026-08-10, reportado pelo usuário tentando anexar uma
 * fatura): {@code @Transactional} no método inteiro (Mongo + MySQL juntos)
 * fazia o Quarkus tentar enlistar a sessão do Mongo na transação JTA aberta
 * pro lado do Hibernate/MySQL — e o servidor Mongo deste projeto roda
 * standalone (não replica set), que rejeita sessão/transação com
 * "Transaction numbers are only allowed on a replica set member or mongos".
 * Corrigido isolando a parte MySQL num bloco {@link QuarkusTransaction}
 * próprio (programático, não anotação — funciona mesmo chamado de dentro da
 * própria classe, ao contrário de {@code @Transactional} em método privado),
 * deixando as chamadas ao Mongo fora de qualquer transação JTA.
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
     * Mongo primeiro, sem transação JTA nenhuma — não precisa (nem
     * suporta, ver Javadoc da classe). Lançamentos (MySQL/Hibernate) num
     * {@link QuarkusTransaction} próprio, requiringNew() garante uma
     * transação nova mesmo que já exista uma ativa por acaso. Bloqueia até
     * commitar de verdade antes de retornar, qualquer que seja o chamador
     * — é isso que ADR-0024 precisa (processamento assíncrono do documento
     * só pode começar depois que salvar() realmente terminou).
     */
    @Override
    public void salvar(DocumentoImportado documento) {
        DocumentoImportadoEntity entity = mongoRepository.findById(documento.getId());
        if (entity == null) {
            mongoRepository.persist(DocumentoImportadoMapper.paraNovaEntidade(documento));
        } else {
            DocumentoImportadoMapper.atualizarEntidade(entity, documento);
            mongoRepository.update(entity);
        }

        QuarkusTransaction.requiringNew().run(() -> salvarLancamentos(documento.getLancamentos()));
    }

    private void salvarLancamentos(List<LancamentoPendente> lancamentos) {
        for (LancamentoPendente lancamento : lancamentos) {
            LancamentoPendenteEntity lancamentoEntity = lancamentoRepository.findById(lancamento.getId());
            if (lancamentoEntity == null) {
                lancamentoRepository.persist(LancamentoPendenteMapper.paraNovaEntidade(lancamento));
            } else {
                LancamentoPendenteMapper.atualizarEntidade(lancamentoEntity, lancamento);
            }
        }
    }

    /**
     * Mongo fora de transação (mesmo motivo do salvar()); os lançamentos
     * (MySQL) precisam de uma transação JTA explícita porque este método
     * também é chamado a partir da thread do {@code ManagedExecutor}
     * durante o processamento em background (ProcessarDocumentoService),
     * onde não existe request HTTP nem transação ativa — sem isso, o
     * Hibernate ORM recusa a leitura (testado na prática:
     * ContextNotActiveException). {@link QuarkusTransaction} programático
     * cobre os dois casos (thread de request e thread de background) sem
     * precisar de {@code @Transactional} (que, além de reintroduzir o
     * problema do Mongo, nem funcionaria numa chamada interna à própria
     * classe).
     */
    @Override
    public Optional<DocumentoImportado> buscarPorId(UUID id, UUID usuarioId) {
        DocumentoImportadoEntity entity = mongoRepository.findById(id);
        if (entity == null || !entity.usuarioId.equals(usuarioId)) {
            return Optional.empty();
        }
        List<LancamentoPendente> lancamentos = QuarkusTransaction.requiringNew().call(() -> buscarLancamentos(id));
        return Optional.of(DocumentoImportadoMapper.paraDominio(entity, lancamentos));
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
