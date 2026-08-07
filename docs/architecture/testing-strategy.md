# Estratégia de Testes

> Regra de `CLAUDE.md`: nenhuma classe de domínio/serviço é entregue sem teste
> correspondente. Este documento define o que testar em cada camada e com
> qual ferramenta, pra não reinventar isso a cada slice.

## 1. Pirâmide

```
        ┌───────────────┐
        │   E2E (poucos) │  fluxo crítico completo via HTTP real
        ├───────────────┤
        │  Integração    │  serviço + banco real (Testcontainers) + contrato OpenAPI
        ├───────────────┤
        │  Unitário      │  domínio, regras de negócio, mapeamento — maioria dos testes
        └───────────────┘
```

Mais testes unitários que de integração, mais integração que E2E. Se uma
regra de negócio pode ser testada sem subir Quarkus/banco, ela deve ser
testada assim — feedback rápido.

## 2. Back-end (Java/Quarkus)

| Camada | Ferramenta | O que cobrir |
|---|---|---|
| Domínio/regras de negócio | JUnit 5 + AssertJ | Toda regra de negócio (ex: "não debita se saldo insuficiente", "fatura vencida não aceita edição") — casos felizes E de borda |
| Uso de dependências externas | Mockito | Mock de portas (`LlmProvider`, clientes REST de outros serviços) — nunca mocka o próprio domínio sendo testado |
| Persistência/API | `@QuarkusTest` + Testcontainers (MySQL/Mongo real em container) | Repositórios, endpoints REST completos, serialização |
| Contrato | Validação do payload contra o YAML em `docs/specs/` | Request/response batem com o que foi speced antes do código |
| Resiliência | `@QuarkusTest` com falha simulada (ex: `account-service` fora do ar) | Retry/timeout/fallback do SmallRye Fault Tolerance se comportam como esperado |

Nome de teste: `deveria<ComportamentoEsperado>_quando<Condicao>` (português,
consistente com o domínio em português definido em `CLAUDE.md`).

## 3. Front-end (React / React Native)

| Camada | Ferramenta | O que cobrir |
|---|---|---|
| Componente | Vitest + React Testing Library | Renderização condicional, interação do usuário, estados de erro/loading |
| Hooks/lógica | Vitest | Lógica extraída de componentes (cálculos, formatação) |
| Integração com API | MSW (Mock Service Worker) | Componente reage certo a resposta real da API (mockada na borda HTTP, não na função) |
| Mobile | React Native Testing Library | Mesmo critério do web, adaptado a componentes RN |

## 4. `ai-service` — cuidado extra

LLMs não são determinísticos — não testar "a resposta exata do modelo".
Testar sim:
- `LlmProvider` é chamado com o prompt/contexto correto (mock do provider).
- Agente escolhe a tool certa dado um tipo de pergunta (casos representativos
  do PRD, seção 3.4) — isso É determinístico e testável.
- Tools MCP retornam o dado certo dado o estado do banco (integração).
- Parsing de documento: usar PDFs de exemplo (fixtures, sem dado real de
  ninguém) e validar que os campos extraídos batem com o esperado.

## 5. Definição de pronto (para qualquer tarefa em `docs/tasks.md`)

- [ ] Código implementado seguindo o contrato em `docs/specs/` (quando
      aplicável).
- [ ] Teste unitário cobrindo a regra de negócio principal e pelo menos um
      caso de borda.
- [ ] Teste de integração se a tarefa envolve banco, fila ou chamada a outro
      serviço.
- [ ] Nenhum dado sensível (valor, descrição de transação, API key) em log.
- [ ] Nenhuma dependência nova com vulnerabilidade HIGH/CRITICAL sem exceção
      documentada (CI gate, ADR-0017).
- [ ] Serviço de aplicação expõe endpoint de healthcheck HTTP (obrigatório
      pra deploy via Kamal, ADR-0021 — no Quarkus, extensão
      `quarkus-smallrye-health` já resolve).
- [ ] `docs/tasks.md` atualizado (item marcado, ou novo item se surgiu
      trabalho extra).
