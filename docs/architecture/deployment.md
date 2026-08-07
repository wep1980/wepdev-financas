# Ambiente de Implantação

> Como e onde o sistema roda de verdade — dev e produção. Nenhum dado
> sensível (IP, domínio real, hostname, credencial, chave SSH) vai neste
> arquivo nem em nenhum outro arquivo versionado — só termos genéricos.
> Segredo de verdade vive em variável de ambiente/Vault, nunca commitado
> (regra já em `CLAUDE.md`).

## 1. Dev

Máquina Windows — é de onde o desenvolvimento acontece.
`docker compose up -d` sobe toda a infra localmente (ver `README.md`).

**Pegadinha de hostname do Keycloak (relevante de novo em produção):** o
Keycloak em `start-dev` sem hostname fixo infere o `issuer` do token pelo
`Host` da requisição — um serviço validando token via rede interna do
Docker (`keycloak:8080`) vê um issuer diferente do que um cliente externo
(Postman, navegador) usando `localhost:8080` recebeu no token, e rejeita
com 401. Corrigido em dev fixando `KC_HOSTNAME` no `docker-compose.yml` +
desabilitando OIDC discovery nos serviços (`quarkus.oidc.discovery-enabled=false`,
paths manuais) pra não seguir as URLs absolutas que o discovery document
passa a devolver. **Em produção esse mesmo problema vai aparecer nas
mesmas condições**: o Keycloak vai precisar de `KC_HOSTNAME` apontando pro
domínio público (o que os usuários acessam via Cloudflare Tunnel, ADR-0019),
e os serviços vão precisar de `KEYCLOAK_ISSUER_URL` apontando pra esse
mesmo domínio público mesmo enquanto continuam falando com o Keycloak pela
rede interna — não esquecer isso quando chegar na fatia 9.

## 2. Produção

Um único servidor Linux próprio do usuário (não é um cluster, não é
multi-nó), acessado via SSH. Levantamento real feito em 2026-08-07:

- **SO**: Ubuntu 24.04.4 LTS, kernel 6.8, x86_64.
- **Recursos**: 12 vCPUs, 15GB RAM (a maior parte livre — 12GB em
  buff/cache, reclamável), ~4GB swap quase sem uso. Disco: partição raiz
  231GB com 140GB livres (37% usado); há outros mounts dedicados a outros
  fins, não relevantes aqui. **Conclusão**: a preocupação de recurso
  registrada em ADR-0016 (stack pesado pra "um servidor pessoal") era
  excessivamente cautelosa — esse servidor tem folga real.
- **Docker**: já instalado (Docker 29.x, Compose v5.x) — sem setup
  necessário.
- **É um host compartilhado por vários projetos**, não só o portfólio: além
  do site de portfólio (container em `127.0.0.1:3000`, não exposto
  diretamente), rodam também Umami (analytics), duas instâncias de
  Postgres, pgAdmin, Portainer (gestão de containers) e **Watchtower**
  (atualiza containers automaticamente quando uma imagem nova é publicada —
  relevante pra CD, ver seção 6). Ou seja: o sistema de finanças precisa
  conviver com outros serviços já rodando, não só um site estático.
- **Acesso público**: via **Cloudflare Tunnel** (`cloudflared` ativo) — não
  é port-forward tradicional. O portfólio, por exemplo, nem escuta em
  `0.0.0.0`, só em `127.0.0.1`; quem expõe pra internet é o túnel. Nginx
  está instalado no host mas o serviço systemd está **inativo**, não é o
  mecanismo em uso. O sistema de finanças usa o mesmo túnel (ADR-0019) —
  hostname público novo apontando pro container de entrada.
- **Firewall (ufw) ativo**, com VPN WireGuard (`10.8.0.0/24`, interface
  `wg0`) como canal principal de acesso administrativo. **SSH roda em porta
  não-padrão (não 22) e só aceita conexão vinda da faixa da VPN** — não é
  alcançável da internet pública nem de um runner hospedado pelo GitHub.
  Isso invalida a suposição original de ADR-0018 ("GitHub Actions conecta
  via SSH direto") — precisa de ajuste, ver seção 6.
- **Portas já em uso por outros projetos no host**: entre outras, 8080 e
  8081 (WordPress e outro projeto, respectivamente) colidem com as portas
  que hoje usamos em *dev* pro `account-service`. **Regra pra produção**:
  nenhum serviço interno nosso publica porta pro host — só o que o
  mecanismo de ingress (túnel ou proxy) precisar; todo o resto fica só na
  rede Docker interna (`financas-net`), do jeito que os outros projetos do
  próprio usuário já fazem com Postgres/Umami.

## 3. Ingress e deploy — resolvido

As duas pendências foram fechadas: ingress via **Cloudflare Tunnel**
(ADR-0019, mesmo mecanismo do portfólio) e deploy via **runner self-hosted
do GitHub Actions** rodando no próprio servidor (ADR-0020, já que SSH é
VPN-only). Nenhuma porta nova precisa ser aberta no firewall pra nenhum dos
dois.

## 4. Bancos de dados e serviços de apoio

Podem rodar em Docker (evoluindo o `docker-compose.yml` já existente) ou
instalados nativamente no servidor — não é uma regra fixa pro projeto
inteiro, é uma decisão por serviço quando fizer sentido (ex: performance,
familiaridade operacional, algo que já esteja instalado por outro motivo).
Ver ADR-0016 pro critério.

## 5. Topologia

Detalhe da decisão de produção em ADR-0016. Diagrama de implantação em
`docs/architecture/diagrams.md` seção 5.

## 6. Como o deploy chega no servidor

GitHub Actions builda a imagem em runner hospedado do GitHub (CI: build +
teste + scan de vulnerabilidade, ADR-0017), publica no GitHub Container
Registry (`ghcr.io`), e o job de deploy roda num **runner self-hosted
instalado no próprio servidor** (ADR-0020) — que puxa o job do GitHub
(conexão outbound), sem precisar de SSH nem porta nova aberta.

Esse job final aciona o **Kamal** (ADR-0021) pros serviços de aplicação
(stateless): sobe a imagem nova ao lado da antiga, só corta o tráfego (via
`kamal-proxy`) depois que a nova passa no healthcheck, mantém a anterior
pronta pra `kamal rollback` se algo der errado. Serviços de dado/infra
(MySQL, Mongo, Redis, Kafka, Qdrant, Keycloak) continuam via
`docker compose pull && up -d` normal (ADR-0016) — não fazem parte do
esquema de troca com segurança do Kamal.

Público chega no serviço via **Cloudflare Tunnel** (ADR-0019), apontando pro
`kamal-proxy` (não direto pro container, que troca a cada deploy). Deploy
só acontece depois que CI passou — nunca pula etapa.
