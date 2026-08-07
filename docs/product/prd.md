# PRD — Sistema de Finanças Pessoais

> Fonte da verdade do *produto*. Decisões de arquitetura ficam em
> `docs/architecture/`; isso aqui é sobre o quê e o porquê, não o como.

## 1. Visão

Dar a uma pessoa uma visão completa e sempre atualizada da sua vida
financeira — contas, cartões, receitas, despesas — sem exigir lançamento
manual de cada transação, e permitir que ela converse com o sistema em
linguagem natural para tomar decisões do dia a dia ("posso gastar R$200 esse
fim de semana?", "quanto gastei com mercado esse mês comparado ao mês
passado?").

O diferencial não é só "planilha bonita": é ler os documentos que o usuário
já recebe (fatura de cartão, extrato bancário) e transformar isso em dado
estruturado automaticamente, e depois deixar uma IA responder perguntas em
cima disso.

## 2. Personas

- **Usuário único (v1)**: alguém organizado o suficiente pra querer controle
  fino, mas sem paciência pra lançar cada compra manualmente. Já tem conta
  corrente, cartão de crédito, talvez mais de um banco.
- **Multi-usuário (evolução)**: mesmo perfil, mas o sistema precisa isolar
  completamente os dados de cada usuário (ver ADR-0003). Não há
  compartilhamento de dados entre usuários (ex. conta conjunta) no escopo
  atual — cada usuário só vê o que é seu.

## 3. Casos de uso principais

### 3.1 Gestão manual de contas e transações
- Cadastrar contas (corrente, poupança, carteira, cartão de crédito,
  investimento).
- Lançar receitas/despesas manualmente, associadas a uma conta e categoria.
- **Editar** uma receita/despesa já lançada (descrição, valor, categoria,
  data). Editar valor de uma transação já `CONFIRMADA` implica ajustar o
  saldo da conta (reverter o efeito antigo, aplicar o novo) — detalhar essa
  regra junto durante a implementação da fatia 1.
- **Excluir** uma receita/despesa: exclusão lógica (`CANCELADA`), nunca
  física — reverte o efeito no saldo se já estava `CONFIRMADA`, mesmo
  princípio de auditabilidade já usado pra contas (seção 4).
- Consultar saldo atual e histórico por período/categoria.
- Transações podem ser **recorrentes**: usuário define valor, frequência
  (v1: só mensal) e duração — número fixo de ocorrências (ex: 24 meses) ou
  indefinida (ex: salário mensal, repete até o usuário cancelar). O sistema
  gera as ocorrências automaticamente ao longo do tempo. Isso é distinto de
  parcelamento de compra no cartão de crédito (conceito do `card-service`,
  atrelado a uma fatura específica) — recorrência aqui é uma regra genérica
  de repetição de lançamento, não presa a cartão. Ver ADR-0009.

### 3.2 Ingestão de documentos financeiros
- Upload de fatura de cartão de crédito em PDF → sistema extrai cada
  lançamento (descrição, valor, data, parcela se houver), o total da fatura
  e a data de vencimento, e gera transações pendentes de confirmação.
- Upload de extrato bancário em PDF ou CSV → mesma lógica, para movimentações
  de conta corrente/poupança.
- Upload de **boleto de financiamento** (PDF ou foto) → sistema extrai valor,
  vencimento e beneficiário, prioritariamente via linha digitável (dado
  determinístico, sem depender de LLM) — ver ADR-0014.
- No **app mobile**, qualquer um desses documentos pode ser enviado por
  **foto** (câmera), além de PDF/CSV — útil pra boleto/fatura em papel. Ver
  ADR-0015.
- A lista de tipos de documento é extensível por design (ver ADR-0004) — "e
  etc" fica aberto pra quando surgir necessidade de um novo tipo.
- Usuário revisa e confirma os lançamentos importados antes deles valerem pro
  saldo (evita poluir os dados com erro de OCR/parsing sem o usuário ver).
- Categorização automática best-effort (por palavra-chave/histórico), com
  opção de correção manual — toda correção manual deveria alimentar uma
  categorização melhor no futuro (fora de escopo v1, ver 5.2).

### 3.3 Orçamento e "quanto posso gastar"
- Definir orçamento por categoria e/ou mês.
- O sistema calcula quanto do orçamento já foi consumido e quanto resta.
- "Disponível pra gastar esse mês" = saldo em conta corrente/carteira −
  compromissos já sabidos do mês (contas fixas, fatura de cartão em aberto)
  − reserva definida pelo usuário, quando aplicável. A regra exata de cálculo
  é responsabilidade do `budget-service` e deve ser documentada lá quando
  implementada.

### 3.4 Assistente em linguagem natural (IA)
- Usuário faz perguntas em português sobre sua própria situação financeira.
- Exemplos de pergunta que o v1 precisa responder corretamente:
  - "Qual valor disponível eu tenho para gastar esse mês?"
  - "Quanto gastei com [categoria] nos últimos 3 meses?"
  - "Minha fatura do cartão X vence quando e quanto é?"
  - "Meus gastos esse mês estão maiores ou menores que o mês passado?"
- Resposta deve ser baseada em dado real do usuário (via RAG sobre
  transações/faturas/orçamento), nunca inventada. Ver
  `docs/architecture/ai-strategy.md`.
- Usuário escolhe o provedor de IA (OpenAI com API key própria, ou Ollama
  local) — ver ADR-0002. Implicação de produto: se o usuário não configurou
  nenhum provedor, a funcionalidade de IA fica desabilitada com mensagem
  clara, o resto do sistema funciona normalmente.

### 3.5 Ações em linguagem natural (IA)

Além de responder perguntas (3.4), a IA pode **executar ações que alteram
dado** — v1: criar receita, criar despesa, e as versões recorrentes das duas
(ver 3.1). Exemplos que o v1 precisa suportar:

- *"Adicione uma nova receita mensal de R$10.000"* → cria uma transação
  recorrente do tipo RECEITA, frequência mensal, sem data fim (indefinida,
  até o usuário cancelar).
- *"Criar uma despesa recorrente de 24 meses no valor de R$19.990"* → cria
  uma transação recorrente do tipo DESPESA, frequência mensal, 24
  ocorrências.

**Canal de entrada:**
- App mobile: comando **falado (voz) ou escrito (texto)**.
- Front-end web: só comando **escrito (texto)** — sem captura de voz no v1.

**Confirmação obrigatória**: a IA nunca persiste uma ação direto a partir do
comando. Ela interpreta o comando, monta um resumo estruturado da ação
proposta (tipo, valor, frequência, duração, conta/categoria quando
aplicável) e só executa depois que o usuário confirma explicitamente na
mesma conversa — mesmo princípio já usado pra importação de documento (3.2),
pela mesma razão: interpretação de linguagem natural erra valor/frequência/
categoria, e erro em dado financeiro tem custo real. Ver ADR-0007.

### 3.6 Alertas de vencimento

Sistema avisa proativamente sobre compromisso financeiro perto de vencer.
Escopo v1:
- Ocorrência de **despesa recorrente** (3.1) se aproximando da data prevista
  (ex: aluguel, assinatura).
- **Fatura de cartão de crédito** se aproximando do vencimento (quando
  `card-service` existir — roadmap #2).

**Canais** (usuário escolhe quais quer usar, em Preferências):
- **Push no app mobile, com som** — canal padrão, sempre disponível.
- **WhatsApp**, para o número pessoal do usuário — via biblioteca não-oficial
  conectada ao WhatsApp do próprio sistema (não a API oficial da Meta). Essa
  escolha é consciente: viola os Termos de Uso do WhatsApp e tem risco real
  de o número usado ser banido — aceitável porque hoje é uso pessoal/poucos
  usuários; se o produto crescer, revisitar pra API oficial. Ver ADR-0012.
- **E-mail**, para o endereço cadastrado do usuário.

**Preferências por usuário**: quais canais estão ativos, com quantos dias de
antecedência quer ser avisado (default: 3 dias antes do vencimento).

**Não-duplicação**: o mesmo vencimento não gera mais de um alerta por canal
— o sistema mantém histórico do que já foi enviado.

### 3.7 Dashboard visual (web)

Página com gráfico mostrando com o que o usuário está gastando mais dinheiro
— gastos por categoria num período selecionável, com comparação ao período
anterior. Mesma agregação usada pela IA na consulta (`resumo_gastos_por_categoria`,
ver `docs/architecture/ai-strategy.md`), exposta como endpoint REST comum
(`GET /transacoes/resumo-por-categoria`) — um cálculo, dois consumidores
(dashboard e IA), sem duplicar lógica.

## 4. Requisitos não-funcionais

- **Privacidade/segurança**: dados financeiros nunca em log claro; API keys
  de LLM do usuário armazenadas de forma segura (nunca em texto plano no
  banco); se o usuário optar por Ollama local, nenhum dado financeiro sai da
  infraestrutura dele. Política completa e inventário de credenciais em
  `docs/architecture/security.md` — requisito não-negociável do sistema.
- **Confiabilidade dos dados importados**: nenhum documento importado altera
  saldo sem confirmação do usuário (ver 3.2). Parsing errado deve ser fácil de
  corrigir, nunca silenciosamente ignorado.
- **Auditabilidade**: nada é apagado fisicamente (contas, transações,
  documentos importados) — exclusão lógica, histórico sempre reconstruível.
- **Multi-tenancy**: isolamento de dados por usuário garantido em nível de
  API (ver ADR-0003), não só em nível de UI.
- **Idioma**: produto e comunicação em português (pt-BR).
- **Entrada por voz (mobile)**: transcrita localmente no dispositivo (SO),
  nunca enviada como áudio pro back-end — só o texto resultante chega no
  sistema, mesmo pipeline do comando escrito. Ver ADR-0008.
- **Risco operacional do canal WhatsApp**: por usar biblioteca não-oficial
  (ADR-0012), o canal pode parar de funcionar sem aviso (número banido,
  biblioteca quebrada por mudança do WhatsApp). Push e e-mail nunca podem
  depender do WhatsApp estar funcionando — são canais independentes.

## 5. Fora de escopo (v1)

1. Conexão automática via Open Finance / API de banco (o usuário sobe o
   arquivo manualmente por enquanto).
2. Contas compartilhadas/conjuntas entre usuários.
3. Investimentos com cálculo de rentabilidade (a conta tipo `INVESTIMENTO`
   existe pra registrar saldo, não pra calcular performance de carteira).
4. Categorização automática por machine learning treinado nos dados do
   usuário (v1 usa regra simples por palavra-chave).
5. Notificações proativas (push/e-mail) — existem como conceito no roadmap
   (`notification-service`) mas não são v1.

## 6. Métricas de sucesso (qualitativas por ora)

- Usuário consegue subir uma fatura de cartão real e ter os lançamentos
  corretos sem edição manual extensa.
- Usuário consegue perguntar "quanto posso gastar esse mês" e receber uma
  resposta correta e rastreável (a IA deveria conseguir explicar de onde tirou
  o número).
