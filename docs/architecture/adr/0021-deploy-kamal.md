# ADR-0021: Deploy dos serviços de aplicação via Kamal (zero-downtime, rollback automático)

Status: Aceita — refina o mecanismo de deploy de ADR-0020
Data: 2026-08-07

## Contexto

O usuário já tinha usado, em outros projetos, um esquema onde toda imagem
nova publicada substitui a antiga automaticamente, mas mantém a versão
anterior pronta pra reverter caso a nova dê problema. O Watchtower — já
rodando no mesmo servidor, cuidando de outros projetos do usuário — não
oferece isso: ele troca a imagem direto, sem checagem de saúde nem
possibilidade de reversão automática. ADR-0020 já tinha resolvido *onde*
o deploy roda (runner self-hosted, sem SSH externo); faltava decidir *como*
o deploy acontece com a segurança que o usuário pediu.

## Decisão

Adotar **Kamal** pra orquestrar o deploy dos **serviços de aplicação**
(stateless): `account-service`, `transaction-service`, os que vierem depois
no domínio, e o front-end Next.js (BFF, ADR-0006). Kamal:

1. Sobe o container com a imagem nova (publicada no `ghcr.io`, ADR-0018) ao
   lado do container antigo — não substitui de cara.
2. Só corta o tráfego pra versão nova depois que ela passa no healthcheck
   configurado (endpoint HTTP, ex: `/q/health` no Quarkus via
   `quarkus-smallrye-health`).
3. `kamal-proxy` (proxy leve que o próprio Kamal instala no host) é quem faz
   esse corte de tráfego — vira o ponto de entrada interno estável de cada
   serviço, na frente do container ativo no momento.
4. Mantém a versão anterior disponível — `kamal rollback` reverte com um
   comando, sem precisar de intervenção manual no Docker.

Kamal roda **a partir do runner self-hosted já no servidor** (ADR-0020) —
conecta via SSH a `localhost`, então não exige nada novo de rede/firewall.

**Serviços de dado/infraestrutura continuam via `docker-compose.yml`**
(MySQL, MongoDB, Redis, Kafka, Qdrant, Keycloak — ADR-0016, inalterado) —
blue-green não se aplica a um banco de dados da mesma forma que a um
serviço stateless; trocar o container de um banco "sem downtime" é um
problema diferente (replicação, migração), fora de escopo aqui. Kamal
suporta um conceito de "accessories" pra gerenciar serviços de apoio
também, mas não é adotado agora — reavaliar só se `docker-compose` deixar
de ser suficiente pra esses serviços.

## Consequências

- Ferramenta nova pra aprender/configurar (`config/deploy.yml` por
  serviço) — trade-off aceito pela segurança de rollback pedida
  explicitamente pelo usuário.
- **Cada serviço de aplicação precisa expor endpoint de healthcheck HTTP**
  — vira requisito de implementação (Quarkus já tem extensão pronta pra
  isso), não só de infra. Adicionado à definição de pronto em
  `testing-strategy.md`.
- Hostname do Cloudflare Tunnel (ADR-0019) aponta pro `kamal-proxy`, não
  direto pro container do serviço — o container por trás troca a cada
  deploy, o proxy é quem fica estável.
- Convive sem conflito com o Watchtower que já cuida dos outros projetos do
  usuário nesse mesmo host — Kamal só gerencia os containers que ele
  próprio sobe.
- Rollback vira operação de segundos (um comando), não um processo manual
  de reverter tag de imagem e reaplicar compose.
