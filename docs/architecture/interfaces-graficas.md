# Interfaces Gráficas

> Todo painel/console web que vem junto com as imagens do `docker-compose.yml`
> ou com cada serviço — pra você navegar/inspecionar sem precisar de
> `curl`/Postman. Nenhum domínio real de produção entra aqui (regra do
> `deployment.md`) — a seção 2 usa `<seu-dominio>` como placeholder.

## 1. Ambiente de dev (o que estamos usando agora)

Depois de `docker compose up -d` (infra) + os serviços rodando
(`mvn quarkus:dev` local ou os containers do próprio compose):

| Interface | URL | Login | Pra que serve |
|---|---|---|---|
| **Keycloak** — admin console | http://localhost:8080 | `admin` / `admin` | Gerenciar realm `financas`: usuários, roles, clients, sessões ativas, ver/editar tudo que hoje fazemos via API |
| **Kafka UI** ([Provectus](https://github.com/provectus/kafka-ui)) | http://localhost:8090 | — (sem auth em dev) | Ver tópicos (`conta.eventos`, `transacao.eventos`), inspecionar mensagens, consumer groups, criar/configurar tópico |
| **Grafana** | http://localhost:3001 | `admin` / `admin` | Dashboards de observabilidade — hoje sem datasource conectado (falta `otel-collector` no compose, ver `docs/tasks.md`) |
| **Prometheus** | http://localhost:9090 | — (sem auth em dev) | Consultar métricas cruas, ver alvos de scrape configurados (`infra/observability/prometheus.yml`) |
| **account-service** — Swagger UI | http://localhost:8081/q/swagger-ui | — | Testar endpoints manualmente, sem Postman |
| **account-service** — Dev UI (só em `quarkus:dev`) | http://localhost:8081/q/dev | — | Painel interno do Quarkus: config ativa, beans CDI, saúde, etc. |
| **transaction-service** — Swagger UI | http://localhost:8082/q/swagger-ui | — | Idem, pro `transaction-service` |
| **transaction-service** — Dev UI (só em `quarkus:dev`) | http://localhost:8082/q/dev | — | Idem |

**Sem interface gráfica própria** (acessar via cliente externo, ex. DBeaver — credenciais em cada README de serviço): MySQL (`localhost:3307`), MongoDB (`localhost:27017`), Redis (`localhost:6379`), Kafka broker direto (`localhost:29092`, use o Kafka UI acima em vez disso), Zookeeper (interno, sem porta exposta ao host).

Todas as credenciais acima são valores de dev (`.env.example`), nunca reaproveitadas em produção — ver `docs/architecture/security.md`.

## 2. Produção (ainda não implantado — fatia 9)

Topologia decidida em `deployment.md`/ADR-0016/ADR-0019: servidor Linux
único, ingress via **Cloudflare Tunnel**, acesso administrativo via **VPN
WireGuard** (mesmo canal já usado pra SSH hoje). Isso implica uma escolha
por interface — **nem tudo que é público em dev deveria ficar público em
produção**:

| Interface | Fica pública (Cloudflare Tunnel) ou só VPN? | Por quê |
|---|---|---|
| Keycloak — fluxo de login/token (`/realms/financas/protocol/...`) | **Pública**, ex. `auth.<seu-dominio>` | Usuário final (web/mobile) precisa autenticar de fora |
| Keycloak — **admin console** (`/admin`) | ⚠️ Em aberto — ver decisão abaixo | Painel sensível: cria usuário, muda role, vê sessão de qualquer um |
| Grafana, Kafka UI, Prometheus | **Só VPN**, mesmo padrão do SSH hoje | Ferramenta de operação interna, sem motivo pra existir na internet pública |
| `account-service` / `transaction-service` (API) | **Pública**, ex. `api.<seu-dominio>` (ou um path por serviço atrás do mesmo hostname) | É o que o front-end/app consome |

**Decisão que falta tomar** (registrei como ponto em aberto em vez de decidir sozinho, porque é trade-off de conveniência × risco, mesma categoria da decisão do WhatsApp no ADR-0012): a Keycloak serve o login público E o console admin na mesma porta — não dá pra simplesmente "não tunelar o admin" sem também derrubar o login. As opções são:

1. **Tunelar tudo, proteger `/admin/*` com Cloudflare Access** (política de autenticação adicional — ex. código por e-mail — só nesse path, antes até da tela de login do Keycloak aparecer). Não exige mudar nada no Keycloak.
2. **Tunelar tudo, sem proteção extra**, confiando só na senha forte do admin (mais simples, mais exposto).
3. **Não tunelar o Keycloak inteiro** — login também vira VPN-only, o que implicaria os usuários finais também precisarem de VPN pra usar o sistema (provavelmente inviável pra um produto real).

Minha recomendação é a opção 1 quando chegarmos na fatia 9 — mas é uma escolha sua, não vou decidir sozinho. Fica registrado aqui pra não esquecer.

### Padrão de URL (produção)

Nenhum domínio real fica em arquivo versionado. Quando o domínio existir de
verdade, o padrão sugerido (ajustar conforme a decisão acima):

```
https://api.<seu-dominio>          → account-service + transaction-service (via kamal-proxy)
https://auth.<seu-dominio>         → Keycloak (login público; /admin conforme decisão pendente)
```

Grafana/Kafka UI/Prometheus não ganham hostname público — acesso via túnel
WireGuard direto na porta interna do servidor (mesmo esquema do SSH hoje).
