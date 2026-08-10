# Diagramas

> Documento vivo — atualizar quando um serviço/entidade/integração novo
> entrar em cena, não só quando "sobrar tempo". Diagramas de fluxo específico
> (sequência de um caso de uso) ficam perto do texto que os explica em
> `docs/architecture/overview.md` e `ai-strategy.md` — aqui ficam os
> diagramas estruturais (visão do todo) e um índice de tudo que existe.

## 1. Contexto (quem fala com o sistema)

```mermaid
graph TB
    Usuario((Usuário))

    Usuario -->|usa| Web[Web — Next.js]
    Usuario -->|usa, texto ou voz| Mobile[App Mobile — React Native]

    Web --> Sistema[Sistema de Finanças Pessoais]
    Mobile --> Sistema

    Sistema --> Keycloak[Keycloak — OIDC]
    Sistema --> OpenAI[OpenAI API]
    Sistema --> Ollama[Ollama — local]
    Sistema --> FCM[Firebase Cloud Messaging]
    Sistema --> WhatsAppLib[WhatsApp — biblioteca não-oficial]
    Sistema --> EmailProvider[Provedor de e-mail transacional]

    style Sistema fill:#4a5568,color:#fff
```

Provedor de IA (OpenAI ou Ollama) e provedor de e-mail são escolhas
configuráveis, não fixas — ver ADR-0002 e ADR-0013.

## 2. Containers / serviços

```mermaid
graph LR
    subgraph Clientes
        Web[Next.js Web]
        Mobile[React Native]
    end

    subgraph "Sistema de Finanças Pessoais"
        AccountSvc["account-service :8081"]
        TxSvc["transaction-service :8082"]
        CardSvc["card-service :8083"]
        DocSvc["document-service :8084"]
        BudgetSvc["budget-service :8085 (planejado)"]
        AiSvc["ai-service :8086 (planejado)"]
        NotifSvc["notification-service :8087 (planejado)"]
        Kafka[("Kafka")]
    end

    subgraph Dados
        MySQL[("MySQL")]
        Mongo[("MongoDB")]
        Qdrant[("Qdrant — proposto ADR-0005")]
        Redis[("Redis — cache")]
    end

    Web --> AccountSvc
    Web --> TxSvc
    Web --> AiSvc
    Web --> DocSvc
    Mobile --> AccountSvc
    Mobile --> TxSvc
    Mobile --> AiSvc
    Mobile --> DocSvc

    TxSvc -->|síncrono| AccountSvc
    DocSvc -->|síncrono, confirma posse ADR-0025| AccountSvc
    DocSvc -->|evento| Kafka
    Kafka --> TxSvc
    TxSvc -->|evento| Kafka
    AiSvc -->|tools MCP| TxSvc
    AiSvc --> BudgetSvc
    AiSvc --> AccountSvc
    AiSvc --> CardSvc
    NotifSvc -->|polling diário, role=service| TxSvc
    NotifSvc -->|polling diário, role=service| CardSvc

    AccountSvc --> MySQL
    TxSvc --> MySQL
    CardSvc --> MySQL
    BudgetSvc --> MySQL
    DocSvc --> MySQL
    DocSvc --> Mongo
    AiSvc --> Qdrant
    AiSvc --> Mongo
    NotifSvc --> Mongo
```

Detalhe de cada serviço (responsabilidade, porta, banco, status) na tabela
de `docs/architecture/overview.md` seção 2 — não duplicado aqui.

## 3. Modelo de domínio

### 3.1 `account-service` + `transaction-service`

```mermaid
erDiagram
    CONTA ||--o{ TRANSACAO : possui
    TRANSACAO_RECORRENTE ||--o{ TRANSACAO : gera

    CONTA {
        uuid id
        uuid usuarioId
        string nome
        string tipo "CORRENTE|POUPANCA|CARTEIRA|CARTAO_CREDITO|INVESTIMENTO"
        decimal saldo
        boolean ativa
    }
    TRANSACAO {
        uuid id
        uuid contaId
        uuid usuarioId
        string descricao
        decimal valor
        string tipo "RECEITA|DESPESA"
        string categoria
        string status "PENDENTE|CONFIRMADA|CANCELADA"
        uuid transacaoRecorrenteId "nullable"
    }
    TRANSACAO_RECORRENTE {
        uuid id
        uuid contaId
        uuid usuarioId
        decimal valor
        string frequencia "MENSAL"
        int quantidadeOcorrencias "nullable = indefinida"
        int ocorrenciasGeradas
        string status "ATIVA|PAUSADA|CANCELADA|CONCLUIDA"
    }
```

Contrato completo (campos, validação) em
`docs/specs/account-service.yaml` e `docs/specs/transaction-service.yaml` —
este diagrama é o mapa, não a fonte da verdade dos campos.

### 3.2 `notification-service`

Banco separado (*database-per-service*, ADR-0001) — sem FK real com o
domínio acima, só referência lógica por id (`origemId`).

```mermaid
erDiagram
    PREFERENCIA_NOTIFICACAO {
        uuid usuarioId
        string canaisHabilitados "PUSH, WHATSAPP, EMAIL"
        int diasAntecedencia
        string telefoneWhatsapp "nullable"
    }
    ALERTA {
        uuid id
        uuid usuarioId
        string origem "TRANSACAO_RECORRENTE|FATURA_CARTAO"
        uuid origemId "referência lógica, sem FK cross-service"
        date dataVencimento
        string canaisEnviados
        datetime enviadoEm
    }
```

### 3.3 `card-service`

Banco separado (`card_db`, ADR-0001) — sem FK real com `account-service`,
só referência lógica (`contaPagamentoId`), confirmada de forma síncrona no
momento de pagar a fatura (ADR-0022).

```mermaid
erDiagram
    CARTAO ||--o{ FATURA : possui
    FATURA ||--o{ PARCELA : contém

    CARTAO {
        uuid id
        uuid usuarioId
        string apelido
        string bandeira "nullable"
        decimal limite
        int diaFechamento
        int diaVencimento
        uuid contaPagamentoId "referência lógica ao account-service"
        boolean ativo
    }
    FATURA {
        uuid id
        uuid cartaoId
        uuid usuarioId
        string competencia "AAAA-MM"
        date dataFechamento
        date dataVencimento
        decimal valorTotal
        string status "ABERTA|FECHADA|PAGA"
    }
    PARCELA {
        uuid id
        uuid faturaId
        uuid compraId "comum a todas as parcelas da mesma compra"
        string descricao
        decimal valor
        string categoria "nullable"
        int numeroParcela
        int quantidadeParcelas
    }
```

Contrato completo em `docs/specs/card-service.yaml`. `Cartao`, `Fatura` e
`Parcela` já têm diagrama de classe (seções 4.4 e 4.5, código
implementado) — este ER conceitual segue sendo o mapa rápido de alto
nível, sem repetir o detalhe de método já coberto lá.

### 3.4 `document-service`

Agregado dividido entre dois bancos (`overview.md`) — metadados +
conteúdo bruto do PDF no MongoDB (`document_service`), lançamentos
queryable no MySQL (`document_db`), sem transação distribuída entre os
dois (ADR-0023, ver `docs/tasks.md` fatia 3 item 2). `contaId` (informado
só no momento da confirmação, ADR-0025) não é campo persistido do
agregado — é passagem direta pro evento Kafka.

```mermaid
erDiagram
    DOCUMENTO_IMPORTADO ||--o{ LANCAMENTO_PENDENTE : contém

    DOCUMENTO_IMPORTADO {
        uuid id "MongoDB, _id"
        uuid usuarioId
        string tipo "FATURA_CARTAO (único valor na fatia atual)"
        string nomeArquivo
        bytes conteudoArquivo "PDF bruto"
        string status "RECEBIDO|PROCESSANDO|AGUARDANDO_CONFIRMACAO|CONFIRMADO|ERRO_PROCESSAMENTO"
        string mensagemErro "nullable"
        datetime criadoEm
        datetime processadoEm "nullable"
    }
    LANCAMENTO_PENDENTE {
        uuid id "MySQL"
        uuid documentoId "referência lógica ao Mongo, sem FK real"
        string descricao
        decimal valor
        date data
        string tipo "RECEITA|DESPESA"
        string categoriaSugerida "nullable, best-effort"
        string status "PENDENTE|CONFIRMADO|REJEITADO"
    }
```

Contrato completo em `docs/specs/document-service.yaml`. Sem diagrama de
classe dedicado (seção 4) — o domínio é enxuto o bastante (`DocumentoImportado`/
`LancamentoPendente`, ver `services/document-service/.../domain`) pra esse
ER conceitual já cobrir o essencial sem repetição.

### 3.5 Pendente

`budget-service` ainda não tem contrato OpenAPI (roadmap #4) — modelo de
domínio dele entra aqui quando a spec for escrita (spec-driven, ver
`CLAUDE.md`), não antes.

## 4. Diagrama de classes

Diferente do modelo de domínio (seção 3, conceitual/ER), aqui é o desenho
das classes de código de verdade — atualizar junto quando o domínio mudar,
não deixar decolar do código.

### 4.1 `account-service` — domínio

```mermaid
classDiagram
    class Conta {
        -UUID id
        -UUID usuarioId
        -String nome
        -TipoConta tipo
        -BigDecimal saldo
        -String instituicao
        -boolean ativa
        -Instant criadoEm
        -Instant atualizadoEm
        +criar(usuarioId, nome, tipo, saldoInicial, instituicao)$ Conta
        +reconstituir(id, usuarioId, ...)$ Conta
        +debitar(valor: BigDecimal) void
        +creditar(valor: BigDecimal) void
        +atualizar(nome: String, instituicao: String) void
        +inativar() void
        +isAtiva() boolean
    }

    class TipoConta {
        <<enumeration>>
        CORRENTE
        POUPANCA
        CARTEIRA
        CARTAO_CREDITO
        INVESTIMENTO
    }

    class SaldoInsuficienteException {
        <<exception>>
        +SaldoInsuficienteException(contaId, saldoAtual, valorSolicitado)
    }

    class ContaNaoEncontradaException {
        <<exception>>
        +ContaNaoEncontradaException(id)
    }

    Conta "1" *-- "1" TipoConta : tipo
    Conta ..> SaldoInsuficienteException : lança em debitar()\nquando saldo < valor

    note for Conta "usuarioId: 1 usuário → N contas, sem limite e sem\nrestrição de unicidade (ver migração V1). Usuário\nNÃO é uma classe de domínio aqui — é gerenciado\npelo Keycloak, entra só como referência (ADR-0003)."
    note for ContaNaoEncontradaException "Não é lançada pela própria Conta — é lançada pelos\ncasos de uso (camada application) quando o id não\nexiste OU quando a conta é de outro usuário (mesmo\nerro nos dois casos, de propósito — evita confirmar\na quem não é dono que aquele id existe, IDOR)."
```

Regras que esse diagrama expressa:

- **1 usuário → N contas.** `usuarioId` é obrigatório e imutável em `Conta`,
  mas não há unicidade — um usuário pode ter quantas contas quiser, de
  qualquer combinação de `TipoConta`.
- **`usuarioId` nunca vem do cliente.** Todo caso de uso que muta ou lê uma
  conta específica (`buscar`, `atualizar`, `excluir`, `criar`) recebe o
  `usuarioId` já resolvido pelo `ContaResource` a partir do claim `sub` do
  token — nunca de um campo de request. Isso é o que garante "o usuário
  existe" (só chega até aqui quem tem token válido do Keycloak) e evita um
  usuário agir em nome de outro.
- **Saldo nunca fica negativo.** `debitar()` valida internamente e lança
  `SaldoInsuficienteException` antes de alterar o estado — não existe
  caminho no código pra saldo ficar negativo.
- **Exclusão é sempre lógica.** `inativar()` marca `ativa=false`; não existe
  método de exclusão física na classe. Idempotente — inativar de novo não
  quebra nada.
- **`atualizar()` só mexe em nome/instituição.** `tipo`, `saldo` e
  `usuarioId` não são editáveis por esse método — saldo só muda via
  `debitar`/`creditar`, e trocar o dono de uma conta não é uma operação que
  existe.
- **Dois construtores, duas intenções.** `criar()` valida como criação nova
  (regras de negócio de entrada); `reconstituir()` é usado só pela
  persistência pra remontar um objeto que já existia, sem reaplicar
  validação de criação.

### 4.2 `transaction-service` — domínio

```mermaid
classDiagram
    class Transacao {
        -UUID id
        -UUID contaId
        -UUID usuarioId
        -String descricao
        -BigDecimal valor
        -TipoTransacao tipo
        -String categoria
        -LocalDate dataTransacao
        -StatusTransacao status
        -UUID transacaoRecorrenteId
        -Instant criadoEm
        +criar(contaId, usuarioId, descricao, valor, tipo, categoria, dataTransacao)$ Transacao
        +reconstituir(id, contaId, ...)$ Transacao
        +atualizar(descricao, valor, categoria, dataTransacao) void
        +cancelar() void
        +isCancelada() boolean
    }

    class TipoTransacao {
        <<enumeration>>
        RECEITA
        DESPESA
    }

    class StatusTransacao {
        <<enumeration>>
        PENDENTE
        CONFIRMADA
        CANCELADA
    }

    class AccountServiceClient {
        <<port>>
        +debitar(contaId, valor) void
        +creditar(contaId, valor) void
    }

    class ContaNaoEncontradaException {
        <<exception>>
        +ContaNaoEncontradaException(contaId)
    }

    class SaldoInsuficienteException {
        <<exception>>
        +SaldoInsuficienteException(contaId)
    }

    class TransacaoNaoEncontradaException {
        <<exception>>
        +TransacaoNaoEncontradaException(id)
    }

    class TransacaoCanceladaException {
        <<exception>>
        +TransacaoCanceladaException(id)
    }

    class IntervaloInvalidoException {
        <<exception>>
        +IntervaloInvalidoException(inicio, fim)
    }

    Transacao "1" *-- "1" TipoTransacao : tipo
    Transacao "1" *-- "1" StatusTransacao : status
    AccountServiceClient ..> ContaNaoEncontradaException : lança
    AccountServiceClient ..> SaldoInsuficienteException : lança em debitar()

    note for TransacaoNaoEncontradaException "Mesmo padrão do ContaNaoEncontradaException:\nid inexistente OU de outro usuário viram o\nmesmo 404 (evita IDOR)."

    note for TransacaoCanceladaException "Lançada por AtualizarTransacaoUseCase quando o\nchamador tenta editar uma transação já CANCELADA\n(mapeada pra 422 — regra de negócio, não erro de input)."

    note for IntervaloInvalidoException "Lançada por ResumoPorCategoriaUseCase quando início\nvem depois de fim (mapeada pra 400 — erro de input,\nnão regra de negócio como as duas acima)."

    note for Transacao "Nasce sempre CONFIRMADA — quando criar() roda, o\nefeito no saldo já aconteceu (RegistrarTransacaoUseCase\nchama o account-service ANTES de persistir). PENDENTE\nfica pra um fluxo futuro (ex: importação de documento).\natualizar() não mexe em contaId/tipo/usuarioId — trocar\nde conta ou tipo é cancelar e recriar (evita ambiguidade\nde reversão de saldo entre contas diferentes)."
    note for AccountServiceClient "Porta pro account-service (chamada síncrona).\nA implementação faz 2 chamadas com 2 tokens diferentes:\nGET /contas/{id} com o token do próprio usuário\n(confirma posse — reusa o 404 do account-service,\nevita IDOR) e POST /contas/{id}/debitos|creditos\ncom token de serviço (client_credentials)."
```

Regras que esse diagrama expressa:

- **Débito/crédito acontece antes de persistir.** `RegistrarTransacaoUseCase`
  (camada application, fora deste diagrama de domínio) chama
  `AccountServiceClient` antes de `Transacao.criar()` — se a chamada
  falhar, nada é salvo (sem transação "fantasma").
- **`AccountServiceClient` é uma porta só de saída.** O domínio não sabe
  que existe HTTP, OIDC ou dois tokens diferentes — isso é tudo detalhe de
  `infrastructure.client` (ver `services/transaction-service/README.md`).
- **`usuarioId` nunca vem do cliente**, mesmo padrão do `account-service`
  (ADR-0003) — extraído do token em `TransacaoResource`.
- **Cancelar reverte o efeito original.** `cancelar()` é só uma mudança de
  estado (idempotente); quem decide reverter o saldo é
  `CancelarTransacaoUseCase`, que chama `AccountServiceClient` com a
  operação oposta à original (`DESPESA` → credita de volta, `RECEITA` →
  debita de volta) **antes** de marcar `CANCELADA` — mesma ordem
  "efeito externo primeiro" usada em `criar()`.
- **Resumo por categoria não chama o `account-service`.**
  `ResumoPorCategoriaUseCase` só lê `Transacao` já persistida (soma
  `DESPESA` `CONFIRMADA` num período, agrupada por categoria) — cálculo
  puramente local, sem efeito colateral externo.

### 4.3 `transaction-service` — `TransacaoRecorrente`

```mermaid
classDiagram
    class TransacaoRecorrente {
        -UUID id
        -UUID contaId
        -UUID usuarioId
        -String descricao
        -BigDecimal valor
        -TipoTransacao tipo
        -String categoria
        -FrequenciaRecorrencia frequencia
        -LocalDate dataInicio
        -Integer quantidadeOcorrencias
        -int ocorrenciasGeradas
        -StatusTransacaoRecorrente status
        -Instant criadoEm
        +criar(contaId, usuarioId, descricao, valor, tipo, categoria, frequencia, dataInicio, quantidadeOcorrencias)$ TransacaoRecorrente
        +reconstituir(id, contaId, ...)$ TransacaoRecorrente
        +proximaDataVencimento() LocalDate
        +registrarOcorrenciaGerada() void
        +cancelar() void
        +isAtiva() boolean
        +isCancelada() boolean
    }

    class FrequenciaRecorrencia {
        <<enumeration>>
        MENSAL
    }

    class StatusTransacaoRecorrente {
        <<enumeration>>
        ATIVA
        PAUSADA
        CANCELADA
        CONCLUIDA
    }

    class TransacaoRecorrenteNaoEncontradaException {
        <<exception>>
        +TransacaoRecorrenteNaoEncontradaException(id)
    }

    TransacaoRecorrente "1" *-- "1" FrequenciaRecorrencia : frequencia
    TransacaoRecorrente "1" *-- "1" StatusTransacaoRecorrente : status
    TransacaoRecorrente "1" ..> "*" Transacao : gera (transacaoRecorrenteId)

    note for TransacaoRecorrente "Distinta de parcelamento de cartão — conceito próprio\ndo card-service, não reaproveita essa classe (ADR-0009).\nregistrarOcorrenciaGerada() conclui automaticamente ao\natingir quantidadeOcorrencias; null = indefinida, nunca\nconclui sozinha (ex: salário mensal)."
    note for TransacaoRecorrenteNaoEncontradaException "Mesmo padrão de TransacaoNaoEncontradaException:\nid inexistente OU de outro usuário viram o\nmesmo 404 (evita IDOR)."
```

Regras que esse diagrama expressa:

- **`CriarTransacaoRecorrenteUseCase` reusa `RegistrarTransacaoUseCase`**
  pra gerar cada ocorrência — mesmo caminho síncrono com o
  `account-service` de uma transação avulsa (debita/credita antes de
  persistir), sem duplicar a lógica de efeito no saldo.
- **`GerarOcorrenciasRecorrentesJob` (scheduler) é um wrapper fino** sobre
  `GerarOcorrenciasRecorrentesUseCase`, que recebe a data "hoje" como
  parâmetro em vez de ler o relógio do sistema — permite testar geração de
  ocorrência, limite de `quantidadeOcorrencias` e regra indefinida sem
  `Thread.sleep` nem tempo real. Gera no máximo 1 ocorrência por regra por
  execução; atraso do job é recuperado incrementalmente nas execuções
  seguintes, não tudo de uma vez.
- **Cancelar não afeta ocorrências já geradas.** `cancelar()` só impede
  novas `Transacao`s de serem criadas pela regra — as que já existem
  continuam normalmente (têm vida própria, editáveis/canceláveis via
  `/transacoes/{id}` como qualquer outra).

### 4.4 `card-service` — `Cartao`

```mermaid
classDiagram
    class Cartao {
        -UUID id
        -UUID usuarioId
        -String apelido
        -Bandeira bandeira
        -BigDecimal limite
        -int diaFechamento
        -int diaVencimento
        -UUID contaPagamentoId
        -boolean ativo
        -Instant criadoEm
        +criar(usuarioId, apelido, bandeira, limite, diaFechamento, diaVencimento, contaPagamentoId)$ Cartao
        +reconstituir(id, usuarioId, ...)$ Cartao
        +atualizar(apelido, bandeira, limite, diaFechamento, diaVencimento, contaPagamentoId) void
        +inativar() void
        +isAtivo() boolean
    }

    class Bandeira {
        <<enumeration>>
        VISA
        MASTERCARD
        ELO
        AMEX
        OUTRA
    }

    class AccountServiceClient {
        <<port>>
        +confirmarPosseDaConta(contaId) void
        +debitar(contaId, valor) void
    }

    class ContaNaoEncontradaException {
        <<exception>>
        +ContaNaoEncontradaException(contaId)
    }

    class CartaoNaoEncontradoException {
        <<exception>>
        +CartaoNaoEncontradoException(id)
    }

    class SaldoInsuficienteException {
        <<exception>>
        +SaldoInsuficienteException(contaId)
    }

    Cartao "1" *-- "0..1" Bandeira : bandeira
    AccountServiceClient ..> ContaNaoEncontradaException : lança
    AccountServiceClient ..> SaldoInsuficienteException : lança em debitar()

    note for Cartao "contaPagamentoId é referência LÓGICA a uma Conta\ndo account-service (CORRENTE/POUPANCA/CARTEIRA,\nnunca CARTAO_CREDITO — evita circularidade), não\numa FK real (database-per-service, ADR-0001). Nunca\né uma TipoConta.CARTAO_CREDITO — card-service é\nindependente desse tipo (ADR-0022)."
    note for AccountServiceClient "Porta pro account-service (chamada síncrona).\nconfirmarPosseDaConta() usa o token do PRÓPRIO\nusuário repassado (PropagarAutorizacaoHeadersFactory)\ncontra GET /contas/{id} — reusa o 404 do\naccount-service, evita IDOR. debitar() (usado só ao\npagar fatura) confirma posse de novo e então debita\ncom token de serviço (client_credentials)."
    note for CartaoNaoEncontradoException "Mesmo padrão anti-IDOR dos outros dois serviços:\nid inexistente OU de outro usuário viram o\nmesmo 404."
```

Regras que esse diagrama expressa:

- **`contaPagamentoId` é confirmado, nunca uma FK real.** `CriarCartaoUseCase`
  e `AtualizarCartaoUseCase` chamam `AccountServiceClient.confirmarPosseDaConta()`
  antes de persistir — sem cartão "órfão" de conta inválida ou de outro
  usuário.
- **Exclusão é sempre lógica.** `inativar()` marca `ativo=false`,
  idempotente; não afeta faturas/compras já existentes.

### 4.5 `card-service` — `Fatura` e `Parcela`

```mermaid
classDiagram
    class Fatura {
        -UUID id
        -UUID cartaoId
        -UUID usuarioId
        -YearMonth competencia
        -LocalDate dataFechamento
        -LocalDate dataVencimento
        -BigDecimal valorTotal
        -StatusFatura status
        +criar(cartaoId, usuarioId, competencia, dataFechamento, dataVencimento)$ Fatura
        +reconstituir(id, cartaoId, ...)$ Fatura
        +adicionarParcela(valor) void
        +fechar() void
        +pagar() void
        +isAberta() boolean
        +isPaga() boolean
    }

    class Parcela {
        -UUID id
        -UUID faturaId
        -UUID compraId
        -String descricao
        -BigDecimal valor
        -String categoria
        -int numeroParcela
        -int quantidadeParcelas
        +criar(faturaId, compraId, descricao, valor, categoria, numeroParcela, quantidadeParcelas)$ Parcela
        +reconstituir(id, faturaId, ...)$ Parcela
    }

    class StatusFatura {
        <<enumeration>>
        ABERTA
        FECHADA
        PAGA
    }

    class FaturaNaoEncontradaException {
        <<exception>>
        +FaturaNaoEncontradaException(id)
    }

    class FaturaAindaAbertaException {
        <<exception>>
        +FaturaAindaAbertaException(id)
    }

    Fatura "1" *-- "1" StatusFatura : status
    Fatura "1" o-- "0..*" Parcela : contém (faturaId)

    note for Parcela "Não existe classe \"Compra\" persistida — uma compra\né só o agrupamento lógico de todas as Parcelas que\ncompartilham o mesmo compraId (LancarCompraUseCase).\nUma compra parcelada gera 1 Parcela por Fatura\n(competências consecutivas)."
    note for Fatura "Nasce ABERTA e com valorTotal=0 — cada Parcela\nlançada incrementa o valorTotal via adicionarParcela().\nUma fatura por (cartaoId, competencia); criada\nautomaticamente por LancarCompraUseCase, sem endpoint\nde criação manual. fechar()/pagar() são idempotentes."
    note for FaturaAindaAbertaException "Lançada por PagarFaturaUseCase quando a fatura\nainda não fechou (sem valorTotal definitivo) —\nmapeada pra 422."
```

Regras que esse diagrama expressa:

- **Fechamento automático, não manual.** `FecharFaturasVencidasJob`
  (`quarkus-scheduler`, cron diário) fecha toda fatura `ABERTA` cuja
  `dataFechamento` já passou — mesmo padrão de núcleo testável +
  wrapper fino já usado em `transaction-service`
  (`GerarOcorrenciasRecorrentesJob`): a lógica testável
  (`FecharFaturasVencidasUseCase`) recebe "hoje" como parâmetro.
- **Pagar é síncrono e idempotente.** `PagarFaturaUseCase` debita
  `valorTotal` da `contaPagamentoId` do cartão **antes** de marcar
  `PAGA` — se o débito falhar (`SaldoInsuficienteException`), a fatura
  continua `FECHADA`, sem "pagamento fantasma". Pagar de novo uma fatura
  já `PAGA` é 204 sem debitar de novo.
- **`GET /faturas/proximos-vencimentos` (role `service`) só considera
  `FECHADA`** — uma fatura `PAGA` não é mais um vencimento pendente,
  `ABERTA` ainda não tem valor definitivo pra alertar o usuário.

## 5. Implantação (produção)

```mermaid
graph TB
    Dev["Ambiente de dev — Windows"] -->|push/PR| GitHub

    subgraph GitHub["GitHub"]
        HostedRunner["Runner hospedado — CI: build/teste/scan (ADR-0017)"]
        Registry["ghcr.io — imagens taggeadas por commit"]
    end

    HostedRunner -->|push da imagem, só se CI passou| Registry

    Internet((Internet)) -->|"HTTPS, TLS termina aqui"| Cloudflare["Cloudflare (Tunnel)"]
    Cloudflare -.->|"conexão outbound, sem porta aberta (ADR-0019)"| Cloudflared

    subgraph Servidor["Servidor Linux único (produção) — também roda portfólio, Umami, Portainer etc."]
        Cloudflared["cloudflared"]
        SelfRunner["Runner self-hosted GitHub Actions (ADR-0020)"]
        Kamal["Kamal — orquestra o deploy (ADR-0021)"]
        Proxy["kamal-proxy — ponto de entrada estável"]
        Portfolio["Site de portfólio — já existente, não pode quebrar"]

        subgraph App["Serviços de aplicação — blue/green via Kamal"]
            Novo["Container novo — só recebe tráfego após healthcheck OK"]
            Antigo["Container anterior — standby, kamal rollback"]
        end

        subgraph Infra["Dados/infra — Docker Compose normal (ADR-0016)"]
            Dados["MySQL/Mongo/Redis/Qdrant/Kafka/Keycloak"]
        end

        Cloudflared --> Portfolio
        Cloudflared --> Proxy
        Proxy --> Novo
        Proxy -.->|standby| Antigo
        SelfRunner -->|"kamal deploy"| Kamal
        Kamal --> Proxy
        Kamal --> App
        App --> Dados
    end

    SelfRunner -.->|"puxa job + imagem (outbound)"| Registry
```

Ver ADR-0016 (topologia, servidor único), ADR-0019 (ingress via Cloudflare
Tunnel), ADR-0020 (runner self-hosted) e ADR-0021 (Kamal, zero-downtime +
rollback) — detalhe completo em `docs/architecture/deployment.md`. Nenhum
nome real de domínio/host aparece aqui de propósito.

## 6. Fluxos de sequência (índice)

Ficam embutidos perto do texto que os explica, não duplicados aqui:

- Ingestão de documento → transação — `overview.md` seção 3.
- Comando em linguagem natural (consulta e ação) — `overview.md` seção 4,
  `ai-strategy.md` seções 5 e 6.
- Alerta de vencimento — `overview.md` seção 5.
