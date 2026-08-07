# ADR-0018: CI/CD via GitHub Actions

Status: Aceita — mecanismo de deploy (etapa CD) substituído por ADR-0020 (runner self-hosted, não SSH direto)
Data: 2026-08-07

## Contexto

O projeto já usa GitHub Actions como ferramenta de CI/CD (stack original,
`README.md`) e já pressupõe CI existindo desde ADR-0017 (gate de
vulnerabilidade em toda mudança). Faltava desenhar o pipeline em si: o que
roda em cada PR, o que roda em produção, e como o deploy chega no servidor
único definido em ADR-0016 (sem Kubernetes, sem cluster — só um host Linux
acessado por SSH).

## Decisão

Dois momentos de pipeline, ambos em GitHub Actions:

### CI — roda em todo PR e push

1. Build do(s) serviço(s)/app(s) que mudaram (path filters — um PR que só
   mexe no `transaction-service` não builda o resto, ver seção "Consequências").
2. Testes (`docs/architecture/testing-strategy.md`) — unitário + integração.
3. Scan de vulnerabilidade (ADR-0017) — OWASP Dependency-Check (Java), `npm
   audit` (Node), Trivy (imagem Docker, só quando a imagem muda).

PR não é mergeável se qualquer etapa falhar — sem exceção manual fora do
processo já definido em ADR-0017.

### CD — roda só em merge/push na branch `main`

1. Build da imagem Docker do(s) serviço(s) que mudaram.
2. Push da imagem pro **GitHub Container Registry (`ghcr.io`)** — escolhido
   por já vir integrado ao GitHub Actions (autentica com o token padrão do
   workflow, sem credencial extra pra configurar) e não exigir conta em
   outro serviço (ex. Docker Hub).
3. Deploy: o workflow conecta via **SSH** no servidor de produção (mesmo
   servidor único de ADR-0016) e roda `docker compose pull && docker compose
   up -d` (ou equivalente) pra aplicar a imagem nova. É deploy **push**
   (o Actions inicia a conexão) — decisão consciente, mais simples que um
   agente pull rodando no servidor, adequado pra um servidor único (ver
   discussão de GitOps/ArgoCD, descartada por ora — sem cluster Kubernetes,
   não há o que ArgoCD sincronizaria).

## Consequências

- **Credenciais novas**: chave SSH de deploy (par de chaves dedicado, não a
  chave pessoal do usuário) e, se o `ghcr.io` for privado, um token de
  leitura — ambos como **GitHub Actions Secrets**, nunca no repositório. Ver
  inventário atualizado em `docs/architecture/security.md`.
- **Path filters obrigatórios**: o repositório tem múltiplos
  serviços/apps (`services/account-service`, `services/transaction-service`,
  futuramente web/mobile) — sem filtro por caminho alterado, todo PR
  rebuildaria/redeployaria tudo, desperdiçando tempo de CI e aumentando risco
  de deploy de algo que não mudou.
- **Rollback**: cada imagem fica taggeada com o SHA do commit no `ghcr.io` —
  reverter é reexecutar o deploy apontando pra tag anterior, ou reverter o
  commit e deixar o pipeline rodar de novo. Não desenhamos blue-green/canary
  agora — desproporcional pra um servidor único de projeto pessoal;
  revisitar se downtime de deploy virar um problema real.
- Deploy só acontece se CI (testes + scan) passou — nunca pula etapa, mesmo
  em produção.
