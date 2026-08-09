package br.com.wepdev.financas.card.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
class ParcelaPanacheRepository implements PanacheRepositoryBase<ParcelaEntity, UUID> {
}
