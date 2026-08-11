# Design System — `web` (front-end)

> Documento vivo — registra os tokens/padrões **realmente implementados**
> em `services/web`, não uma especificação aspiracional. Cresce junto com
> cada tela nova, mesmo espírito de `docs/tasks.md`/`docs/historico.md`:
> se um componente não existe ainda, ele não entra aqui. Ver a discussão
> completa de escopo (por que este documento é deliberadamente menor que
> um "design system enterprise") em `docs/historico.md`, entrada
> "refinamento visual" de 2026-08-10.

## 1. Princípios

Referências de qualidade visual: Stripe, Vercel, Linear, Notion, Apple —
usadas como **calibre de padrão**, nunca copiadas literalmente. O que se
importa dessas referências:

- **Restrição antes de recurso.** Cada elemento visual (cor, sombra,
  borda, animação) precisa justificar sua existência — o padrão é
  neutro/discreto, cor e ênfase são exceção pontual, não decoração.
- **Hierarquia por espaço e tipografia, não por enfeite.** Diferenciar
  "importante" de "secundário" com tamanho de fonte, peso, cor de texto
  (`--foreground` vs `--muted-foreground`) e espaçamento — não com caixas
  coloridas, gradientes ou ícones decorativos.
- **Densidade de informação baixa por tela, alta por sessão de uso.** O
  usuário deve entender uma tela em segundos; profundidade vem de
  navegar pra outra tela, não de empilhar tudo numa só.
- **Consistência de padrão vale mais que originalidade por tela.** Um
  padrão de lista, um padrão de formulário (dialog + `useActionState`),
  um padrão de confirmação (`ConfirmActionButton`) — reaproveitados em
  toda tela nova, não reinventados.

Este documento **não** é a especificação completa dos 40+ componentes
de um design system enterprise (Data Grid, Command Palette, Timeline,
Step Wizard, Drawer, Context Menu...). Esses componentes não têm uso
nenhum no produto hoje — construí-los e documentá-los antecipadamente
contradiz o princípio de fatias verticais do `CLAUDE.md`. Cada
componente novo entra aqui **quando uma tela real precisar dele**, mesmo
ritmo já seguido desde o item 4 (Dialog só entrou quando o CRUD de conta
precisou; Chart só quando o dashboard precisou).

## 2. Stack de implementação

- **Tailwind CSS v4** — tokens via `@theme inline` em `app/globals.css`,
  não config file separado (convenção da v4).
- **shadcn/ui** (estilo `base-nova`, primitivos `@base-ui/react`) —
  componentes copiados pro repo em `components/ui/`, nunca dependência
  de runtime. Customizados diretamente no arquivo copiado quando
  necessário (ex: `Card` ganhou `shadow-sm` — ver seção 6).
- **Lucide React** — única biblioteca de ícones do projeto (já vinha
  como dependência do shadcn/ui).
- **Inter** (`next/font/google`) — fonte principal, ver seção 4.

## 3. Cor

Paleta neutra/profissional com azul de destaque — decisão original do
item 1 (`docs/tasks.md`, confirmada pelo usuário via pergunta direta),
mantida no refinamento visual do item 8. **Deliberadamente restrita**:
cor tem significado, não é decoração.

| Token | Uso | Luz | Escuro |
|---|---|---|---|
| `--background`/`--foreground` | Fundo/texto padrão da página | quase branco / quase preto | quase preto / quase branco |
| `--card` | Fundo de `Card` | branco | cinza escuro (`oklch(0.205 0 0)`) |
| `--primary` | Ações principais, links ativos, foco, valores positivos | azul `oklch(0.546 0.185 259.8)` | azul mais claro (contraste em fundo escuro) |
| `--destructive` | Erros, ações destrutivas, valores negativos (despesa) | vermelho | vermelho mais claro |
| `--muted-foreground` | Texto secundário, legendas, timestamps | cinza médio | cinza médio |
| `--border`/`--input` | Bordas discretas, linhas divisórias | cinza muito claro | branco a 10-15% opacidade |
| `--sidebar*` | Menu lateral (fundo, texto, item ativo) | ligeiramente diferente do `--background` (destaca a sidebar do conteúdo) | idem |

**Uso semântico de cor** (não é "paleta de marca" espalhada pelo
produto — é sinalização financeira específica):

- Despesa: `text-destructive` (vermelho) com prefixo "-".
- Receita: `text-primary` (azul) com prefixo "+". *Não* verde — mantém
  consistência com o `--primary` já usado em botões/links/foco, evita
  introduzir uma terceira cor de destaque só pra esse caso. Se o
  usuário achar pouco intuitivo na prática de uso, revisitar.
- Valor negativo em qualquer contexto (saldo estourado, orçamento
  estourado): `text-destructive`.
- Nunca usar cor decorativa (ex: card colorido por categoria, ícone
  colorido por seção do menu) — todo uso de cor além de
  `--primary`/`--destructive`/`--muted-foreground` precisa de
  justificativa registrada aqui.

Dark mode **não é inversão automática** — cada token tem valor OKLCH
próprio pros dois temas (`:root` vs `.dark` em `app/globals.css`),
calibrado pra contraste, não só "preto vira branco".

## 4. Tipografia

**Inter** (`next/font/google`, variável `--font-sans`) — trocada da
Geist do scaffold original no refinamento visual (item 8). Achado real
nessa troca: a variável do Geist se chamava `--font-geist-sans`, mas o
`@theme inline` do Tailwind (herdado do init do shadcn/ui) referenciava
`--font-sans` — nomes diferentes, então a fonte customizada **nunca
esteve de fato aplicada** nas telas dos itens 1-7 (caía no fallback
padrão do navegador). Corrigido nomeando a variável do `Inter()`
exatamente `--font-sans`.

- Pesos usados: 400 (texto padrão), 500 (`font-medium`, ênfase leve/
  rótulos ativos), 600 (`font-semibold`, títulos), 700 (reservado,
  ainda sem uso real).
- Números financeiros sempre com `tabular-nums` (dígitos de largura
  fixa — alinhamento visual em coluna, essencial pra escaneabilidade
  de valores monetários). Já aplicado em todo valor monetário desde o
  item 4.
- `font-mono` (Tailwind default — `ui-monospace`, sem fonte própria
  carregada) só usado no tooltip do gráfico (`components/ui/chart.tsx`,
  herdado do componente shadcn).

## 5. Espaçamento e grid

Escala do Tailwind (múltiplos de 4px) usada sem customização — já cobre
a progressão 4/8/12/16/24/32/48/64 pedida como referência:

| Classe Tailwind | px | Uso típico neste projeto |
|---|---|---|
| `gap-1`/`p-1` | 4 | espaçamento mínimo (ex: label + input) |
| `gap-2`/`p-2` | 8 | itens relacionados próximos |
| `gap-3`/`p-3` | 12 | campos de formulário dentro de um dialog |
| `gap-4`/`p-4` | 16 | padding interno de linha de lista, gap entre cards |
| `gap-6`/`p-6` | 24 | padding de página no mobile, gap entre seções |
| `p-8` (`md:p-8`) | 32 | padding de página no desktop — "respiro" maior em tela grande |
| `p-16` | 64 | telas vazias/hero (ex: `/login`) |

Container de página padrão: `p-6 md:p-8` (todas as páginas dentro do
grupo `(app)`) — 24px no mobile, 32px no desktop. Decisão do
refinamento visual (item 8): "muito espaço em branco, nada apertado".

## 6. Border radius e sombra

- `--radius` base = `0.75rem` (12px) — subido de 10px (default do
  shadcn) no refinamento visual. `--radius-lg` (botões, inputs, cards,
  itens de lista) = 12px; `--radius-xl` (dialogs) ≈ 17px (escala
  `--radius * 1.4`, fórmula herdada do shadcn). Nunca cantos retos —
  todo elemento de UI usa uma dessas classes de radius.
- `Card` (`components/ui/card.tsx`, customizado no refinamento visual):
  `shadow-sm` (sombra sutil) + `ring-1 ring-foreground/5` (borda mais
  discreta que o default do shadcn, que era `/10`). "Sombra
  extremamente suave, borda discreta" — não os dois no máximo ao mesmo
  tempo, um sutil complementa o outro.
- Containers de lista (ver seção 8) usam só `border` (sem sombra) —
  mais "chatos"/planos de propósito, reservando sombra pra elementos
  que precisam se destacar da página (Card de métrica, dialog).

## 7. Estrutura da aplicação (shell)

Menu lateral fixo no desktop (`≥768px`, breakpoint `md`), off-canvas no
mobile — trocado do header horizontal do item 3 no refinamento visual
do item 8, alinhado às referências (Linear/Vercel/Notion/YNAB usam
sidebar, não header horizontal com nav inline).

- `app/(app)/app-sidebar.tsx` (Client Component): estado de
  aberto/fechado só no mobile (`useState`, sem `localStorage`/cookie —
  decisão deliberada de simplicidade, não é o bloco "sidebar" completo
  do shadcn/ui, que tem collapse-to-icon/atalho de teclado/persistência
  — infraestrutura de dashboard enterprise sem uso real nos 5 itens de
  menu atuais). CSS puro (`md:` breakpoint) pro estado desktop, não hook
  JS de detecção de mobile — evita flash de layout errado no primeiro
  paint.
- Item de menu ativo: comparação de `usePathname()` com o `href` do
  item, destacado com `bg-sidebar-accent`.
- Item ainda não implementado (`lib/nav-items.ts`, flag
  `implementado: false`): aparece no menu como texto desabilitado +
  rótulo "em breve", nunca como link morto.
- Usuário logado + botão sair ficam no rodapé da sidebar (não mais num
  header separado) — menos elementos de chrome na tela.

## 8. Padrões de página já estabelecidos

Repetidos de tela em tela (itens 4-7) — qualquer tela nova segue estes
padrões antes de inventar um novo:

- **Cabeçalho de seção**: `<h1>`/`<h2>` (`text-xl font-semibold
  tracking-tight` pro título de página, `text-lg` pra subtítulo de
  seção) + botão de ação principal alinhado à direita
  (`flex items-center justify-between`).
- **Lista de itens** (contas, transações, documentos, orçamentos):
  container `rounded-lg border divide-y`, cada linha
  `flex items-center justify-between px-4 py-3` — nome/descrição à
  esquerda (com legenda secundária em `text-muted-foreground text-sm`
  embaixo), valor/status + ações à direita.
- **Formulário de criar/editar**: `Dialog` com um único componente
  reutilizado pros dois modos (`modo: "criar" | "editar"` via prop),
  `useActionState` pro estado de pending/erro, fecha e limpa sozinho
  ao detectar sucesso (transição `pending: true → false` sem erro).
- **Ação destrutiva/irreversível** (excluir, cancelar): sempre via
  `components/confirm-action-button.tsx` — `confirm()` nativo do
  navegador antes de submeter, nunca uma ação de um clique só.
  Extraído no item 5 depois de aparecer duplicado duas vezes.
- **Filtro simples** (conta/período): `<form method="GET">` nativo,
  sem JavaScript — funciona com JS desabilitado, sem componente client
  nenhum.
- **Estado vazio**: frase única em `text-muted-foreground`, nunca uma
  ilustração ou componente dedicado — ainda não há tela complexa o
  bastante pra justificar isso.
- **Card** (`components/ui/card.tsx`): reservado pra blocos de métrica/
  resumo (dashboard) — não usado em lista de itens (ver seção 6).
- **Processamento em background sem % real** (`components/ui/progress.tsx`):
  barra indeterminada (faixa animada indo e voltando, `role="progressbar"`)
  — nunca fingir uma porcentagem que não existe. Usado hoje só em
  `/documentos/[id]` enquanto a fatura processa (RECEBIDO/PROCESSANDO).

## 9. Acessibilidade

- Primitivos `@base-ui/react` (Dialog, Select) já cobrem foco
  preso/restaurado, `aria-*`, navegação por teclado — herdado de graça
  ao usar os componentes shadcn/ui, sem trabalho extra por tela.
- Foco visível: `focus-visible:ring-3 focus-visible:ring-ring/50` já no
  `Button`/`Input` padrão do shadcn/ui.
- Contraste: paleta neutra + azul já testada visualmente nos dois temas
  desde o item 1; nenhuma auditoria formal WCAG rodada ainda —
  pendência conhecida, não bloqueante pro estágio atual do produto.

## 10. O que fica de fora, de propósito (por enquanto)

Itens do pedido original de design system que **não** foram construídos
nesta passada, com o motivo — revisitar apenas quando uma tela real
precisar:

| Item pedido | Por que não agora |
|---|---|
| Command Palette, Data Grid, Timeline, Step Wizard, Context Menu, Drawer | Nenhuma tela do produto precisa hoje |
| Header com busca/notificações | Sem `notification-service` (ainda 🔲 no roadmap) nem busca implementada — ícone sem função é decorativo |
| Menu com Metas, Investimentos (CRUD dedicado), Relatórios, Importações, Exportações, Categorias (tela separada) | Sem serviço de back-end nem item de PRD correspondente |
| Paleta "de marca" (verde/roxo por tipo de dado) | Mantido cor semântica mínima (azul=positivo, vermelho=negativo) — ver seção 3 |
| Documentação de todo estado de todo componente (40+) | Documentado só o que existe (seção 8); cresce por tela, não por especificação antecipada |

## 11. Changelog

- **2026-08-11 — barra de progresso indeterminada**: novo componente
  `components/ui/progress.tsx`, usado em `/documentos/[id]` durante o
  processamento assíncrono da fatura — ver seção 8.
- **2026-08-10 — refinamento visual (fatia 6, item 8)**: paleta
  mantida, radius 10px→12px, `Card` ganhou sombra sutil, fonte
  Geist→Inter (corrigindo bug latente de variável CSS não conectada),
  shell header→sidebar, padding de página `p-6`→`p-6 md:p-8`. Ver
  `docs/historico.md` pra decisões e achados detalhados.
