package br.com.wepdev.financas.ai.domain;

/** Usuário nunca configurou provedor de IA (ProvedorIa.NENHUM) — erro de negócio claro, mapeado pra 422 (ai-strategy.md seção 1). */
public class IaNaoConfiguradaException extends RuntimeException {

    public IaNaoConfiguradaException() {
        super("Nenhum provedor de IA configurado — configure em PUT /api/v1/configuracao antes de usar o chat");
    }
}
