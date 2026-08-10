package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.domain.ProvedorIa;
import jakarta.validation.constraints.NotNull;

/** provedor tipado como enum — Jackson já recusa (400) valor fora de OPENAI/OLLAMA/NENHUM, sem precisar de validação extra. */
public record ConfiguracaoIaRequest(
        @NotNull ProvedorIa provedor,
        String apiKey,
        String ollamaUrl
) {
}
