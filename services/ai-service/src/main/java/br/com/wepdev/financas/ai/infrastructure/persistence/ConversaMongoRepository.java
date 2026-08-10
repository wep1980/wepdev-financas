package br.com.wepdev.financas.ai.infrastructure.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
class ConversaMongoRepository implements PanacheMongoRepositoryBase<ConversaEntity, UUID> {
}
