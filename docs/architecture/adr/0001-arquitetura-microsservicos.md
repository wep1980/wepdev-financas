# ADR-0001: Microsserviços em vez de monólito

Status: Aceita
Data: 2026-08-06

## Contexto

O sistema começa com uso de uma pessoa (single-user na prática, embora
multi-usuário no design — ver ADR-0003), o que normalmente pesaria a favor de
um monólito modular por simplicidade e velocidade de entrega. Por outro lado,
um dos objetivos explícitos do projeto é usar e demonstrar as tecnologias
mais adotadas no mercado (microsserviços, Kafka, Keycloak, Kubernetes), e já
existe trabalho iniciado nessa direção (specs OpenAPI de `account-service` e
`transaction-service`, infra em `docker-compose.yml`).

## Decisão

Manter arquitetura de microsserviços, *database-per-service*, comunicação
síncrona (REST) para consistência imediata e eventos assíncronos (Kafka)
entre domínios. Ver `docs/architecture/overview.md` para a lista de serviços.

## Consequências

- Mais complexidade operacional (múltiplos bancos, broker de eventos, service
  discovery implícito via nomes de container/K8s) do que um monólito
  justificaria pelo volume de uso real.
- Cada fatia vertical (roadmap) precisa decidir se o novo serviço se justifica
  como serviço separado ou se cabe dentro de um existente — não criar serviço
  por criar. Ex: `card-service` é separado porque tem ciclo de vida próprio
  (fatura, parcelamento); `document-service` é separado porque tem
  dependência pesada diferente (parsing/LLM) do resto.
- Se a complexidade operacional virar o principal ponto de atrito do projeto
  (mais tempo mantendo infra do que construindo funcionalidade), revisitar
  esta decisão com um ADR novo — não é uma decisão irreversível.
