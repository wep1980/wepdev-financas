# ADR-0028: Upload de fatura de cartão passa a integrar com o card-service (supersede parte da ADR-0023)

Status: Aceita
Data: 2026-08-11

## Contexto

Pedido do usuário (ver `docs/historico.md`, 2026-08-11): ao subir uma nova
fatura de cartão, o sistema deve considerar só as compras novas (evitar
duplicar o que já foi lançado num upload anterior) e as compras parceladas
devem continuar reduzindo a quantidade de parcelas restantes mês a mês,
**independente de novo upload** — ex: uma compra de 10 parcelas de
R$100,00 deve aparecer com 9 parcelas restantes no vencimento seguinte,
mesmo sem o usuário subir uma fatura nova.

A ADR-0023 (fatia 3, escopo inicial do `document-service`) já havia
identificado exatamente essa possibilidade e decidiu conscientemente
**não** integrar com o `card-service` na primeira fatia, adiando a
decisão pra quando/se o usuário pedisse esse cruzamento — o que é
exatamente o que está acontecendo agora.

Investigação antes de desenhar a solução: o `card-service` (fatia 2, já
entregue) **já implementa** o comportamento pedido — `Cartao`
(`diaFechamento`/`diaVencimento`), `Fatura` (uma por cartão+competência) e
`Parcela` (agrupada por `compraId`, `numeroParcela`/`quantidadeParcelas`).
`LancarCompraUseCase` já distribui uma compra parcelada em parcelas
consecutivas, criando as faturas futuras sob demanda; o job agendado
`FecharFaturasVencidasJob` já fecha a fatura vencida sozinho, sem
depender de nenhuma ação do usuário. Ou seja, "parcela decrescendo mês a
mês sem novo upload" é exatamente o motor que já existe — construir uma
segunda implementação desse comportamento dentro do `document-service`
duplicaria lógica financeira já testada (82+ testes) e criaria risco dos
dois motores divergirem com o tempo.

## Decisão

1. **Upload de fatura de cartão passa a exigir `cartaoId`** (cartão já
   cadastrado no `card-service`, dono do usuário autenticado) — antes não
   existia esse vínculo.
2. **Confirmação de uma fatura importada não publica mais evento Kafka
   pro `transaction-service`.** Em vez disso, `document-service` chama
   `POST /api/v1/cartoes/{id}/compras` (já existente) do `card-service`
   pra cada compra **nova** encontrada na fatura — à vista
   (`quantidadeParcelas=1`) ou parcelada. O item extraído do PDF ganha
   campos estruturados `numeroParcela`/`quantidadeParcelas` (extraídos
   pelo LLM do texto "Parcela X/Y", com fallback determinístico via regex
   em `AgenteExtracaoFaturaService` se o LLM não preencher).
3. **Dedup por assinatura**: antes de lançar, `document-service` consulta
   `GET /api/v1/cartoes/{id}/compras` (novo endpoint, `ListarComprasUseCase`
   no card-service) e ignora qualquer item cuja assinatura (descrição-base
   sem o sufixo de parcela + valor da parcela + quantidadeParcelas) já
   bate com uma compra existente pro mesmo cartão — não existe
   `compraId` estável impresso no PDF, então esse é o critério prático
   disponível. Sem esse casamento, cada fatura mensal recriaria a mesma
   compra parcelada do zero.
4. **Entrar no meio de uma sequência de parcelas** (primeiro upload do
   usuário mostra, por exemplo, "Parcela 8/11" — a compra começou antes
   dele usar o sistema): registra só as parcelas **restantes**
   (`quantidadeParcelas - numeroParcela + 1`), com valor total
   recalculado a partir do valor da parcela observado. Consequência
   aceita: a numeração exibida daí em diante reinicia (viraria "1/4" em
   vez de continuar "9/11") — cosmético, não afeta valor nem timing.
5. **Sem transação/despesa direta no `transaction-service` pra fatura de
   cartão.** O dinheiro só sai da conta quando o usuário paga a fatura
   explicitamente (`PagarFaturaUseCase`, já existente, síncrono com
   `account-service`) — modelo mais correto financeiramente do que debitar
   no momento da compra (que é o que a "despesa única" da ADR anterior a
   esta fazia). Isso também significa: gasto de cartão não aparece em
   `resumoPorCategoria` do `transaction-service` até a fatura ser paga —
   ai-service precisa combinar as duas fontes pra responder "categoria
   que mais gastei" (ver tarefa de IA no `docs/tasks.md`).
6. `AccountServiceClient`/`ContaNaoEncontradaException`/
   `confirmarPosseDaConta` saem do `document-service` (só existiam pra
   esse fluxo, ver ADR-0025) — a checagem de posse agora é implícita: o
   `card-service` já rejeita `lancarCompra`/`listarCompras` com 404 se o
   `cartaoId` não pertencer ao usuário autenticado (mesmo padrão de
   `usuarioIdAutenticado()` usado em todo o projeto). `contaId` sai do
   `ConfirmarLancamentosRequest` — quem paga a fatura é uma propriedade
   do `Cartao` (`contaPagamentoId`), não uma escolha feita na hora de
   confirmar a importação.

Isso **supersede o item 1 da ADR-0023** ("sem integração com
card-service") especificamente pra fatura de cartão — o item 2 dessa ADR
(PDFBox pra extração de texto) continua valendo, sem mudança.

## Consequências

- `document-service` ganha uma dependência nova (`CardServiceClient`,
  REST client com o mesmo padrão de propagação de token já usado nos
  outros serviços) — não tinha nenhuma integração com `card-service`
  antes.
- O resultado de confirmar uma fatura de cartão deixa de ser "uma
  despesa apareceu na tela de Transações" e passa a ser "as compras
  apareceram no cartão, a fatura ainda precisa ser paga depois" — muda a
  expectativa de UX (front-end precisa comunicar isso claramente, ver
  fatia de front-end).
- `EXTRATO_BANCARIO`/`BOLETO_FINANCIAMENTO` (tipos de documento ainda não
  implementados) continuam indo pro fluxo antigo (evento Kafka →
  `transaction-service`) quando forem construídos — essa decisão é
  específica de `FATURA_CARTAO`, não de todo `document-service`.
- Dedup por assinatura (descrição+valor+quantidadeParcelas) é uma
  heurística, não uma chave garantida — duas compras genuinamente
  diferentes com a mesma descrição/valor/parcelamento no mesmo cartão
  seriam incorretamente tratadas como a mesma (falso positivo,
  aceitável pro MVP; falso negativo — a mesma compra não ser reconhecida
  por pequena variação de texto do LLM entre uploads — é o risco
  oposto, mitigado comparando só a parte fixa da descrição, sem o sufixo
  de parcela).
