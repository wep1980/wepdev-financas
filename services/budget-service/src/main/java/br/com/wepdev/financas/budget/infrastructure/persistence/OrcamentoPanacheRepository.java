package br.com.wepdev.financas.budget.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
class OrcamentoPanacheRepository implements PanacheRepositoryBase<OrcamentoEntity, UUID> {
}
