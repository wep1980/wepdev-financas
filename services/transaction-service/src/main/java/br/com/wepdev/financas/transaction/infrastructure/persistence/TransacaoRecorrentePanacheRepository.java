package br.com.wepdev.financas.transaction.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
class TransacaoRecorrentePanacheRepository implements PanacheRepositoryBase<TransacaoRecorrenteEntity, UUID> {
}
