# ADR-0014: Boleto de financiamento no escopo de ingestão; parsing via linha digitável

Status: Aceita — complementa ADR-0004
Data: 2026-08-06

## Contexto

ADR-0004 tinha fixado o escopo inicial de ingestão em fatura de cartão (PDF)
e extrato bancário (PDF/CSV). O usuário pediu explicitamente pra incluir
**boleto de financiamento** também. Diferente de fatura/extrato — que são
texto livre, formato varia por banco, exige LLM pra interpretar — o boleto
bancário brasileiro tem um formato padronizado (FEBRABAN): a **linha
digitável** (47 ou 48 dígitos) codifica de forma determinística banco, valor
e vencimento. Ou seja, dá pra extrair o dado certo sem depender de LLM, desde
que a linha digitável esteja legível no documento.

## Decisão

`document-service` ganha boleto como terceiro tipo de documento suportado.
Estratégia de extração em duas camadas:
1. **Primária**: localizar e decodificar a linha digitável (via OCR
   direcionado só nos dígitos, ou extração de texto do PDF quando ela vem
   como texto real) — determinístico, sem LLM, sem ambiguidade de valor/data.
2. **Fallback**: se a linha digitável não for legível (foto ruim, boleto
   cortado), cai no mesmo agente de parsing via LLM já usado pra
   fatura/extrato (`docs/architecture/ai-strategy.md`).

Isso reforça a decisão já tomada em ADR-0004 de separar "extração bruta" de
"interpretação em lançamento" — a extração bruta de um boleto só tem uma
estratégia a mais (decodificar linha digitável) antes de cair no caminho
genérico.

## Consequências

- Precisa de uma lib/algoritmo de validação e decodificação de linha
  digitável (formato é público/padronizado, não depende de provedor
  externo) — baixo risco técnico comparado a parsing de fatura em PDF livre.
- Fluxo de confirmação do usuário (PRD 3.2) continua obrigatório mesmo pro
  caminho determinístico — dado errado de OCR nos dígitos ainda é possível
  (ex. dígito confundido), só menos provável que no caminho por LLM.
- Não cobre boleto de concessionária (água/luz, formato de linha digitável
  ligeiramente diferente) explicitamente aqui — mesma lib deve suportar os
  dois formatos (bancário e de concessionária), verificar na implementação.
