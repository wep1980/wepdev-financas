package br.com.wepdev.financas.document.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída (Dependency Inversion) — domínio define o contrato,
 * infraestrutura implementa. A implementação persiste metadados no MongoDB
 * e lançamentos no MySQL (ver {@code overview.md}), mas esse é um detalhe
 * que o domínio e os casos de uso não precisam conhecer.
 */
public interface DocumentoRepository {

    /** Insere se for novo, atualiza se já existir (upsert por id). */
    void salvar(DocumentoImportado documento);

    /** Vazio se não existir OU se pertencer a outro usuário (mesmo tratamento, evita IDOR). */
    Optional<DocumentoImportado> buscarPorId(UUID id, UUID usuarioId);

    /** Documentos do usuário, mais recente primeiro. statusFiltro nulo = sem filtro. */
    List<DocumentoImportado> listarPorUsuario(UUID usuarioId, StatusDocumento statusFiltro);
}
