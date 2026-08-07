# ADR-0007: Comandos de IA que alteram dado exigem confirmação explícita

Status: Aceita
Data: 2026-08-06

## Contexto

O usuário definiu um novo caso de uso (PRD 3.5): a IA pode executar ações
que alteram dado — criar receita, despesa, ou versões recorrentes das duas —
a partir de comando em linguagem natural, falado (mobile) ou escrito
(mobile/web). Interpretação de linguagem natural erra (valor, frequência,
duração, categoria) — e diferente de uma consulta (que só erra a resposta
mostrada), uma ação errada persiste dado financeiro incorreto. Esse é o
mesmo risco que já motivou o fluxo de confirmação de documento importado
(ADR-0004, PRD 3.2).

## Decisão

Toda ação de IA que cria/altera/cancela dado financeiro segue um fluxo de
dois passos, nunca executa direto:

1. O agente interpreta o comando e responde com um **resumo estruturado da
   ação proposta**, sem persistir nada.
2. Só executa a mutação real (chama a tool de escrita, ex: `criar_transacao`)
   depois de confirmação explícita do usuário, na mesma conversa.

Uma proposta não confirmada expira depois de um tempo curto — evita
confirmação tardia agindo sobre contexto desatualizado (ex: usuário confirma
dias depois, sem lembrar o que exatamente foi proposto). Detalhe do fluxo em
`docs/architecture/ai-strategy.md` seção 4.2.

## Consequências

- Mais uma volta de conversa antes de qualquer ação efetivar — UX mais lenta
  que "faz direto", trade-off aceito conscientemente por segurança de dado
  financeiro.
- `ai-service` precisa manter estado de curta duração de "ação pendente de
  confirmação" associado à conversa (histórico em MongoDB).
- Consistência: o mesmo princípio (nunca mutar sem confirmação humana) agora
  vale tanto pra documento importado (ADR-0004) quanto pra comando de IA —
  um único padrão de UX pra "dado que a IA/parsing gerou automaticamente".
- Se no futuro fizer sentido pular confirmação pra comandos de baixíssimo
  risco (ex: valor pequeno, categoria já usada antes), isso é decisão de
  produto nova e explícita — não implícita por este ADR.
