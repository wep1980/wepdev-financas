package br.com.wepdev.financas.document.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
class LancamentoPendentePanacheRepository implements PanacheRepositoryBase<LancamentoPendenteEntity, UUID> {

    List<LancamentoPendenteEntity> listarPorDocumento(UUID documentoId) {
        return list("documentoId = ?1 order by data", documentoId);
    }
}
