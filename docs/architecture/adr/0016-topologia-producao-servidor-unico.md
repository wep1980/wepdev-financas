# ADR-0016: Produção em servidor Linux único, com Docker Compose + reverse proxy

Status: Aceita — detalhe de reverse proxy substituído por ADR-0019 (Cloudflare Tunnel)
Data: 2026-08-06

## Contexto

O usuário informou que produção não vai ser um cluster nem um provedor
gerenciado — é **um servidor Linux próprio, único**, acessado via SSH, que
já roda outro site (portfólio pessoal) exposto na web. O roadmap original
(#9, "Infra avançada") previa evolução pra Kubernetes + Helm + Terraform +
ArgoCD + Istio + Vault — plano que fazia sentido pensando em "tecnologia de
mercado" de forma abstrata, mas precisa ser confrontado com a realidade de
recursos (um servidor pessoal, não um cluster) e com o fato de que já existe
algo rodando ali que não pode quebrar.

## Decisão

Produção roda em **Docker Compose** no servidor único, evoluindo o
`docker-compose.yml` já existente (um `docker-compose.prod.yml` ou overlay
equivalente, não um arquivo novo do zero). Um **reverse proxy** (Traefik ou
Nginx — escolha final quando chegarmos na fatia de deploy, depende de como o
portfólio já está servido, ver `deployment.md`) fica na frente de tudo,
roteando por subdomínio/path com TLS, sem afetar o site existente.

Banco de dados e serviços de apoio (MySQL, Mongo, Redis, Kafka, Qdrant) por
padrão em Docker, junto do resto — instalar algo nativamente no host é
decisão pontual, não a regra.

**Kubernetes/Helm continuam no roadmap (#9), mas deixam de ser "o próximo
passo natural depois do Compose"** — viram uma evolução condicional: só faz
sentido se o projeto justificar (mais de um nó, necessidade real de
orquestração que o Compose não dá) ou se o objetivo for explicitamente
aprender/demonstrar K8s por si só. Não é bloqueante pra nada hoje.

## Consequências

- Produção fica bem mais simples de operar (um `docker compose up -d` no
  servidor, mesma ferramenta usada em dev) — reduz a distância entre dev e
  prod, reduz superfície de erro.
- Precisa de disciplina de recurso: o stack completo (Kafka+Zookeeper, MySQL,
  Mongo, Redis, Keycloak, Prometheus, Grafana, Qdrant, 7 microsserviços) é
  pesado pra um servidor pessoal que já divide recurso com outro site.
  Conforme os serviços forem sendo implementados de verdade, medir uso real
  de RAM/CPU e cortar o que não se justificar rodar sempre ligado (ex.:
  Prometheus/Grafana podem não precisar estar de pé o tempo todo numa fase
  inicial).
- Reverse proxy é pré-requisito de qualquer coisa exposta à internet nesse
  servidor — nenhum serviço novo pode assumir que vai ter porta própria
  liberada direto, como acontece hoje em dev.
- Se o projeto crescer a ponto de precisar de mais de um servidor, revisitar
  esta decisão com ADR novo — não uma reescrita, uma evolução.
