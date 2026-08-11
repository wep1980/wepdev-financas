# Estratégia de IA — `ai-service`

> Decisões confirmadas: abstração de provedor desde o início (ADR-0002),
> vector store Qdrant (ADR-0005, confirmado em 2026-08-10). Este
> documento detalha o desenho técnico em cima dessas decisões.

## 1. Princípio central

O `ai-service` nunca acopla direto a um SDK de LLM. Existe uma porta
(interface) `LlmProvider`, com adapters por provedor. Isso é SOLID aplicado
(Dependency Inversion) e é o que permite trocar OpenAI ↔ Ollama por
configuração, sem tocar em lógica de negócio.

```java
public interface LlmProvider {
    ChatResponse chat(ChatRequest request);
    EmbeddingResult embed(String text);
    boolean isConfigured();
}
```

`ChatRequest` pode carregar um anexo de imagem opcional (proposta ADR-0015,
pra suportar ingestão de documento por foto no mobile) — nem todo provedor/
modelo configurado vai suportar isso; `isConfigured()` (ou uma variação,
ex. `suportaVisao()`) decide se a funcionalidade de foto fica disponível pro
usuário.

- `OpenAiLlmProvider` — usa API key do usuário (armazenada de forma segura,
  nunca em texto plano), chama a API da OpenAI.
- `OllamaLlmProvider` — chama uma instância Ollama (local ou apontada pelo
  usuário), sem custo por token, dados não saem da infraestrutura do usuário.
- Seleção do provedor: configuração por usuário (cada usuário escolhe o seu),
  resolvida em runtime, não em build-time.
- Se `isConfigured()` for falso pro provedor escolhido, a funcionalidade de
  IA retorna erro de negócio claro ("configure sua chave de IA em
  Configurações"), o resto do sistema continua funcionando — é isso que dá
  ADR-0002 na prática.

## 2. RAG — o que é indexado e por quê

Fonte de verdade dos números continua sendo o banco relacional
(`transaction-service`, `budget-service`) — RAG não substitui isso, ele dá
contexto textual/semântico pra IA entender a pergunta e decidir o que
consultar.

O que vira embedding:
- Descrição de transações (para busca semântica: "gastos com mercado" deve
  encontrar transações com descrição "Supermercado Pão de Açúcar" mesmo sem
  match exato de palavra).
- Lançamentos extraídos de faturas/extratos.
- Metadados de orçamento (categoria, limite, período).

O que **não** vira embedding, é buscado direto no banco relacional pela
ferramenta certa (ver seção 4 — MCP tools): saldo atual, valor exato
disponível, totais e somas. Número exato nunca deveria vir "aproximado" de um
vetor — vem de query.

## 3. Vector store — proposta (ADR-0005)

Proposta: **Qdrant**, self-hosted via Docker Compose, junto dos outros bancos.
Motivo: leve, API simples, boa adoção de mercado pra RAG, não exige conta
cloud (ao contrário de MongoDB Atlas Vector Search, que precisaria do Atlas
gerenciado — e o MongoDB da stack atual é self-hosted). Alternativas
descartadas e o porquê estão em ADR-0005 — revisar se o volume de dados ou
requisito de latência mudar.

## 4. Agentes e MCP tools

`ai-service` expõe um **agente orquestrador** que recebe a pergunta em
linguagem natural e decide quais ferramentas chamar. As ferramentas são
expostas via MCP (Model Context Protocol), o que permite reuso tanto pelo
agente interno quanto por um cliente MCP externo (ex: Claude Desktop do
próprio usuário, se ele quiser plugar direto):

| Tool MCP | Faz o quê | Chama |
|---|---|---|
| `buscar_saldo_disponivel` | Retorna valor exato disponível pra gastar no mês | `budget-service` + `account-service` |
| `buscar_transacoes` | Busca transações por período/categoria/texto (híbrido: filtro relacional + busca semântica) | `transaction-service` + Qdrant |
| `buscar_fatura_cartao` | Retorna dados da fatura fechada mais recente (total, vencimento) | `card-service` |
| `resumo_gastos_por_categoria` | Agrega gastos por categoria num período, com comparação a período anterior | `transaction-service` (`GET /transacoes/resumo-por-categoria` — mesmo endpoint usado pelo dashboard web, PRD 3.7, um cálculo só) |
| `compras_parceladas` | Compras parceladas ativas — quantas, maior parcela, quanto falta de cada uma | `card-service` (`GET /cartoes/{id}/compras`, agrupado por `compraId`) |
| `valor_fatura_mes` | Valor da fatura de um mês específico, separado em parcelado vs à vista | `card-service` (fatura da competência + parcelas dela) |
| `categoria_que_mais_gastou` | Categoria com mais gasto num período — soma transações **e** compras de cartão (cartão não gera `Transacao`, ver ADR-0028 do `document-service`) | `transaction-service` + `card-service` |
| `capacidades_do_assistente` | "No que você pode me ajudar" — resposta fixa, sem chamada externa | — |
| `criar_transacao` | **(escrita)** Cria receita/despesa, pontual ou recorrente (`TransacaoRecorrente` se houver frequência/duração — ver ADR-0009) | `transaction-service` |

`criar_transacao` é a única tool de escrita do v1 (PRD 3.5). Todas as outras
são somente leitura. Essa distinção importa pro design do agente — ver 4.2.

As quatro últimas tools de consulta (2026-08-11, pedido do usuário — ver
`docs/historico.md`) nasceram junto com o religamento do `document-service`
ao `card-service` (ADR-0028): antes disso não fazia sentido perguntar sobre
parcelamento porque fatura de cartão importada virava despesa avulsa, sem
nenhum conceito de parcela guardado em lugar nenhum.

Agentes especializados (v1, mínimo necessário — não criar agente por criar):

- **Agente orquestrador**: interpreta a mensagem do usuário, decide se é
  **consulta** (4.1) ou **ação** (4.2), escolhe tool(s), monta resposta
  final. É o único que fala com o usuário.
- **Agente de parsing de documento** (usado pelo `document-service`, não pelo
  chat): dado um PDF de fatura/extrato, extrai lançamentos estruturados. Vive
  logicamente perto do `document-service` mas usa o mesmo `LlmProvider`.

Não há plano de "múltiplos agentes conversando entre si" em v1 — um
orquestrador com tools bem definidas resolve os casos de uso do PRD sem a
complexidade extra de multi-agente.

### 4.1 Intent: consulta

Fluxo de leitura, sem efeito colateral — é o caso coberto na seção 5 abaixo.

### 4.2 Intent: ação (cria/altera dado)

Diferente de consulta, uma ação **nunca executa na primeira resposta**
(ADR-0007). Fluxo:

1. Usuário envia comando ("criar uma despesa recorrente de 24 meses no valor
   de R$19.990").
2. Agente orquestrador identifica intent = ação, extrai parâmetros
   estruturados (tipo RECEITA/DESPESA, valor, frequência, quantidade de
   ocorrências ou indefinida, categoria/conta se mencionados ou a definir).
3. Agente responde com um **resumo da ação proposta em linguagem natural**
   ("Vou criar uma despesa recorrente de R$19.990,00/mês, por 24 meses,
   começando em [data]. Confirma?") — nada é persistido ainda.
4. Essa proposta fica em estado "pendente de confirmação", associada à
   conversa, com expiração curta (evita confirmar depois de muito tempo,
   agindo sobre contexto desatualizado).
5. Só quando o usuário confirma explicitamente ("sim", "confirmar", etc.), o
   agente chama a tool `criar_transacao` de fato.
6. Se o usuário corrigir algo em vez de confirmar ("não, é 30 meses"), o
   agente atualiza a proposta e repete o passo 3 — não assume, não persiste
   parcialmente.

Isso vale igual pra comando escrito (mobile/web) e comando que chegou como
texto transcrito de voz (mobile) — pro `ai-service` os dois são
indistinguíveis, é sempre texto (ver 6 e ADR-0008).

## 5. Fluxo completo — exemplo (consulta)

Pergunta: *"Qual valor disponível eu tenho para gastar esse mês?"*

1. `ai-service` recebe a pergunta + `usuarioId` (do token).
2. Agente orquestrador identifica que precisa de valor exato → chama tool
   `buscar_saldo_disponivel` (não é uma busca semântica, é dado exato).
3. Tool consulta `budget-service` (regra de cálculo do "disponível") e
   `account-service` (saldo atual).
4. Resultado volta pro agente, que monta a resposta em linguagem natural com
   o número e uma explicação curta de como chegou nele (rastreável, conforme
   métrica de sucesso do PRD).
5. Resposta + trace de quais tools/dados foram usados são persistidos no
   histórico de conversa (MongoDB) — permite auditoria de "de onde veio essa
   resposta".

## 6. Fluxo completo — exemplo (ação)

Comando (mobile, falado ou escrito): *"Adicione uma nova receita mensal de
R$10.000"*

1. App mobile transcreve localmente se foi voz (ADR-0008) — `ai-service`
   recebe só texto, igual seria vindo do teclado ou do web.
2. Agente orquestrador identifica intent = ação (4.2), extrai: tipo=RECEITA,
   valor=10000.00, frequência=MENSAL, quantidadeOcorrencias=indefinida.
3. Agente responde: "Vou criar uma receita recorrente de R$10.000,00 por
   mês, sem data de término. Qual conta e categoria devo usar?" (se a conta/
   categoria não foi dita, o agente pergunta em vez de assumir um valor
   default arriscado).
4. Usuário responde ("conta corrente, categoria salário") → agente atualiza a
   proposta e confirma resumo final.
5. Usuário confirma → agente chama `criar_transacao` → `transaction-service`
   cria a `TransacaoRecorrente` (ADR-0009) e a primeira ocorrência.
6. Resposta final ao usuário confirma o que foi criado, com id/link pra
   revisar depois.

## 7. Segurança e privacidade

- API key do usuário (se OpenAI): armazenada criptografada, nunca logada,
  nunca enviada de volta em resposta de API.
- Se usuário usa Ollama: nenhuma chamada sai da rede configurada por ele —
  `ai-service` não deve ter fallback silencioso pra um provedor cloud.
- Todo prompt montado pro LLM inclui apenas dados do `usuarioId` autenticado
  — nunca dado de outro usuário, mesmo por engano de contexto (isolar isso é
  responsabilidade do agente orquestrador antes de montar o prompt).
- Ações (4.2) só executam após confirmação explícita do próprio usuário
  autenticado na mesma conversa — o agente nunca confirma por conta própria,
  mesmo com alta confiança na interpretação (ADR-0007).
- Áudio de voz nunca trafega nem é armazenado no back-end — só o texto
  transcrito no dispositivo (ADR-0008), o que reduz a superfície de dado
  sensível que o `ai-service` precisa proteger.
