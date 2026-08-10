package br.com.wepdev.financas.transaction.infrastructure.messaging;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/** Precisa de uma subclasse concreta — o deserializer genérico do Quarkus não sabe o tipo alvo por causa de type erasure. */
public class DocumentoLancamentosConfirmadosDeserializer extends ObjectMapperDeserializer<DocumentoLancamentosConfirmadosEvento> {

    public DocumentoLancamentosConfirmadosDeserializer() {
        super(DocumentoLancamentosConfirmadosEvento.class);
    }
}
