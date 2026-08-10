package br.com.wepdev.financas.document.infrastructure.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
class DocumentoImportadoMongoRepository implements PanacheMongoRepositoryBase<DocumentoImportadoEntity, UUID> {
}
