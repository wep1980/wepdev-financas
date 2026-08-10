package br.com.wepdev.financas.ai.domain;

import java.util.UUID;

/** Um resultado de busca semântica — score é a similaridade (0 a 1, maior = mais parecido). */
public record ResultadoBusca(UUID id, String texto, float score) {
}
