package br.com.wepdev.financas.document.infrastructure.persistence;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de persistência (MongoDB) — deliberadamente separado de
 * {@link br.com.wepdev.financas.document.domain.DocumentoImportado}. Guarda
 * o documento bruto (bytes do PDF) e os metadados de processamento; os
 * {@code LancamentoPendente} ficam à parte, no MySQL
 * (LancamentoPendenteEntity) — ver docs/architecture/overview.md.
 */
@MongoEntity(collection = "documentos_importados")
public class DocumentoImportadoEntity {

    @BsonId
    public UUID id;

    public UUID usuarioId;

    public String tipo;

    public String nomeArquivo;

    public byte[] conteudoArquivo;

    public String status;

    public String mensagemErro;

    public Instant criadoEm;

    public Instant processadoEm;
}
