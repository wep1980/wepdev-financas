package br.com.wepdev.financas.ai.infrastructure.messaging;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/** Precisa de uma subclasse concreta — o deserializer genérico do Quarkus não sabe o tipo alvo por causa de type erasure. */
public class TransacaoRegistradaDeserializer extends ObjectMapperDeserializer<TransacaoRegistradaEvento> {

    public TransacaoRegistradaDeserializer() {
        super(TransacaoRegistradaEvento.class);
    }
}
