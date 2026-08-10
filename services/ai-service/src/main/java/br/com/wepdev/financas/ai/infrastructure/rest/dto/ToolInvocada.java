package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.application.RegistroTrace;

public record ToolInvocada(String nome, String resumo) {
    public static ToolInvocada de(RegistroTrace registro) {
        return new ToolInvocada(registro.nome(), registro.resumo());
    }
}
