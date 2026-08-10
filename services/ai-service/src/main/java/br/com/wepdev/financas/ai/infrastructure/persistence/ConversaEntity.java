package br.com.wepdev.financas.ai.infrastructure.persistence;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de persistência (MongoDB) — deliberadamente separado de
 * {@link br.com.wepdev.financas.ai.domain.Conversa}. Um documento por
 * conversa, mensagens e a ação pendente embutidas — sem coleção própria
 * pra cada uma (diferente do split usado em outros agregados deste
 * sistema, ex. document-service).
 */
@MongoEntity(collection = "conversas")
public class ConversaEntity {

    @BsonId
    public UUID id;

    public UUID usuarioId;

    public Instant iniciadaEm;

    public List<MensagemEmbedded> mensagens = new ArrayList<>();

    public AcaoPendenteEmbedded acaoPendente;
}
