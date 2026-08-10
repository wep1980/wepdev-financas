package br.com.wepdev.financas.ai.infrastructure.persistence;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.UUID;

/**
 * Modelo de persistência (MongoDB) — deliberadamente separado de
 * {@link br.com.wepdev.financas.ai.domain.ConfiguracaoIa}. Um documento
 * por usuário (usuarioId é a própria chave), sempre upsert — mesmo
 * desenho de ReservaEntity no budget-service. {@code apiKeyCriptografada}
 * nunca guarda texto plano (ver CriptografiaService) — só o mapper de
 * repositório (com acesso a essa dependência) sabe converter de/pra
 * {@code ConfiguracaoIa.apiKey}, não este mapper estático.
 */
@MongoEntity(collection = "configuracoes_ia")
public class ConfiguracaoIaEntity {

    @BsonId
    public UUID usuarioId;

    public String provedor;

    public String apiKeyCriptografada;

    public String ollamaUrl;
}
