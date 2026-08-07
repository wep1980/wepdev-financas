# ADR-0004: Prioridade de ingestão — fatura PDF e extrato PDF/CSV

Status: Aceita — escopo de documento ampliado por ADR-0014 (boleto) e ADR-0015 (foto)
Data: 2026-08-06

## Contexto

Existem múltiplos formatos possíveis de documento financeiro: fatura de
cartão (quase sempre PDF), extrato bancário (pode ser OFX — formato
estruturado padrão de bancos brasileiros — PDF ou CSV). OFX seria
tecnicamente mais fácil de parsear (formato estruturado), mas o usuário
priorizou explicitamente PDF de fatura e PDF/CSV de extrato como primeiro
alvo, por serem os formatos que ele efetivamente tem disponível/usa.

## Decisão

`document-service` implementa parsing de fatura de cartão (PDF) e extrato
bancário (PDF/CSV) primeiro. Suporte a OFX fica no roadmap como melhoria
futura (é estritamente mais simples de adicionar depois, dado que o desenho
do serviço já separa "extração de texto/estrutura do arquivo" de "geração de
lançamento candidato" — ver `docs/architecture/overview.md` seção 3).

## Consequências

- Parsing de PDF é mais frágil que parsing de OFX (formato livre por banco,
  exige extração de texto + heurística/LLM em vez de parser determinístico) —
  aceito conscientemente; é por isso que o fluxo de confirmação manual do
  usuário (PRD seção 3.2) é obrigatório e não um "nice to have".
- `document-service` deve ser desenhado com uma etapa clara de "extração bruta
  do arquivo" separada de "interpretação em lançamentos", pra permitir trocar
  a estratégia de extração (ex: parser específico por banco, ou fallback via
  LLM) sem afetar o resto do pipeline.
