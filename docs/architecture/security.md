# Segurança e Confiabilidade

> Política central: "nunca expor credencial sensível, sistema seguro e
> confiável" não é um objetivo vago — é uma lista de regras concretas e um
> inventário do que precisa de cuidado. Isso aqui consolida o que já estava
> espalhado em vários ADRs; onde a regra já tem dono (ex: multi-tenancy é
> ADR-0003), este documento aponta pra lá em vez de duplicar.

## 1. Gestão de segredos — regra única, sem exceção

Nenhuma credencial, chave de API, token ou senha aparece em: código-fonte,
log (nenhum nível, nem DEBUG), documento versionado (`docs/`, ADR, PRD),
`docker-compose.yml` com valor real, ou mensagem de commit.

- **Dev local**: variável de ambiente via `.env` (gitignored) — ver
  `.env.example` na raiz, que documenta as variáveis com valores de
  placeholder óbvios (`admin`, `financas`), nunca segredo real.
- **Produção**: HashiCorp Vault (já na stack, `README.md`) injeta as
  variáveis de ambiente no `docker-compose.prod.yml`/overlay (ADR-0016) — os
  defaults de dev (`admin`, `root`, etc.) **nunca** são usados em produção.
- **`.gitignore`** (raiz) bloqueia `.env`, `*.pem`, `*.key`, `secrets/` — se
  precisar de um novo tipo de arquivo sensível, adicionar o padrão lá antes
  de criar o primeiro arquivo desse tipo.

## 2. Inventário de credenciais do sistema

Lista viva — todo ADR que introduz uma integração nova com segredo próprio
deve resultar numa linha nova aqui.

| Credencial | Origem | Onde vive |
|---|---|---|
| Senha de root/usuário MySQL | Infra | `.env` (dev) / Vault (prod) |
| Usuário/senha root MongoDB | Infra | `.env` (dev) / Vault (prod) |
| Admin do Keycloak | Infra | `.env` (dev) / Vault (prod) |
| Client secret Keycloak (role `service`, client credentials) | ADR-0003 | Vault — cada serviço lê o seu |
| Senha admin Grafana | Infra | `.env` (dev) / Vault (prod) |
| API key OpenAI (por usuário) | ADR-0002 | Banco do `ai-service`, campo criptografado — nunca em texto plano, nunca logada, nunca retornada em resposta de API |
| Chave de service account do Firebase (FCM) | ADR-0011 | Vault |
| Sessão autenticada do WhatsApp (Baileys) | ADR-0012 | É, na prática, uma credencial — dá acesso de enviar mensagem como o número configurado. Guardar como segredo (Vault/volume protegido), nunca versionada. Ver risco operacional já registrado em ADR-0012 |
| Credencial do provedor de e-mail transacional | ADR-0013 (proposta) | Vault |
| Credencial de acesso SSH ao servidor de produção (pessoal, do usuário) | ADR-0016 / `deployment.md` | Fora do sistema — gerenciada pelo usuário, nunca toca o repositório |
| Token de registro do runner self-hosted do GitHub Actions | ADR-0020, superada por ADR-0030 | Não utilizado; runner self-hosted não será instalado para este projeto |
| `NVD_API_KEY` (chave da API do NIST/NVD, pro scan OWASP Dependency-Check) | ADR-0017, `.github/workflows/ci.yml` | GitHub Actions Secret (nos dois stores: Actions e Dependabot). Ativa e funcionando desde 2026-08-08 — scan roda até o fim. Gratuita, gerar em https://nvd.nist.gov/developers/request-an-api-key |
| Publicação no `ghcr.io` | ADR-0018/0030 | `GITHUB_TOKEN` temporário do próprio job, com `packages: write`; nenhuma credencial persistente no repositório |
| Credencial para propor atualização no `servidor-gitops` | ADR-0030 | Pendente: GitHub App ou token de escopo mínimo, armazenado somente no GitHub Actions Secrets; nunca versionado |
| Token do túnel Cloudflare (Cloudflare Zero Trust) | ADR-0019 | Já gerenciado pelo usuário fora do repositório (mesmo mecanismo do portfólio) — nunca commitado |
| Chave SSH do Kamal | ADR-0021, superada por ADR-0030 | Não utilizada; Kamal deixou de ser o mecanismo de deploy deste projeto |
| `AUTH_SECRET` (assina/criptografa o cookie de sessão do Auth.js no `web`) | ADR-0027 | `.env.local` (dev, default gerado documentado como não-sensível) / Vault (prod) |
| Chave SSH dedicada pro Claude Code acessar o servidor de produção (ed25519, sem passphrase, uso: diagnóstico/preparação de infra, ex. instalação de GPU) | 2026-08-11 (sessão de fechamento da fatia 6) | Par de chaves fora do repositório, só na máquina de dev do usuário (`~/.ssh/id_ed25519_wepdev_financas_servidor` + alias em `~/.ssh/config`, nenhum dos dois versionado). Escopo restrito no `authorized_keys` do servidor via `from="<faixa da rede Wi-Fi do usuário>"` — só autentica vindo de dentro dessa rede, mesmo com a chave pública exposta. Revogar apagando a linha correspondente em `~/.ssh/authorized_keys` do usuário `wepdev` no servidor. Exigiu mudar `AuthenticationMethods password` → comentado (`authenticationmethods any` no efetivo) em `/etc/ssh/sshd_config` — o servidor antes só aceitava senha, mesmo com `PubkeyAuthentication yes` ligado; backup do config original ficou em `/etc/ssh/sshd_config.bak-<data>` no próprio servidor. **Atualização 2026-08-11 (mesmo dia)**: usuário decidiu dar `sudo` **irrestrito, sem senha** (`wepdev ALL=(ALL) NOPASSWD: ALL`) pro usuário `wepdev` — decisão explícita dele depois que eu apresentei o trade-off (escopo restrito a comandos específicos vs. total); escolheu total pra eliminar a fricção de ficar repassando comando por comando durante a instalação da GPU/driver. Arquivo em `/etc/sudoers.d/claude-code` (permissão 440, validado com `visudo -c`). Consequência real: a proteção de rede (`from=`) continua sendo a única barreira — se a chave privada vazar E a rede Wi-Fi do usuário for alcançada, o acesso resultante é root irrestrito no servidor, não mais limitado a diagnóstico |

## 3. Autenticação e autorização

Keycloak (OIDC), roles `usuario`/`admin`/`service`, isolamento de dado por
`usuarioId` do token — regra completa em ADR-0003. Não duplicado aqui.

## 4. Transporte

- **Produção**: TLS obrigatório, terminado no reverse proxy (ADR-0016) — nada
  exposto à internet sem HTTPS.
- **Dev local**: HTTP simples em `localhost` é aceitável (não exposto à
  rede externa).

## 5. Confiabilidade do dado financeiro

Não é uma seção separada de "segurança" por acaso — dado financeiro errado
é tão grave quanto dado financeiro vazado. Já resolvido em outros documentos,
listado aqui só como checklist de referência:

- Exclusão lógica, nunca física (`CLAUDE.md`, princípio 5).
- Nenhuma mutação de dado automática (parsing de documento, comando de IA)
  acontece sem confirmação explícita do usuário (ADR-0004, ADR-0007).
- Débito/crédito de saldo é síncrono com retry/timeout, nunca "otimista"
  (`overview.md` seção 8).
- Auditoria: toda transação recorrente/alerta/ação de IA fica rastreável
  (quem, quando, a partir de quê) — ver `ai-strategy.md` seção 5 (histórico
  de conversa) e `notification-service.yaml` (`AlertaResponse`).

## 6. Dependências e vulnerabilidades

Regra: evitar biblioteca com vulnerabilidade conhecida, e manter isso
verdadeiro ao longo do tempo (não só no dia em que a lib foi escolhida). Ver
ADR-0017 pro detalhe completo. Resumo:

- **Dependabot** monitora Maven, npm e imagens Docker, abre PR de
  atualização sozinho.
- **CI falha** em vulnerabilidade HIGH/CRITICAL sem exceção documentada:
  OWASP Dependency-Check (Java), `npm audit --audit-level=high` (Node),
  Trivy (imagem Docker).
- Vulnerabilidade sem correção disponível → exceção documentada (motivo +
  data de revisão), nunca suprimida em silêncio.
- Isso cobre CVE catalogada — não substitui o julgamento sobre lib
  não-oficial/não mantida (caso do WhatsApp, ADR-0012), que é um risco
  estrutural diferente, gerido à parte.

## 7. Logging

Nunca logar, em nenhum nível: valor de transação, descrição de transação,
qualquer credencial da seção 2, conteúdo de mensagem de IA com dado
financeiro do usuário. Log de erro deve referenciar id (`transacaoId`,
`usuarioId`), nunca o conteúdo sensível em si — já em `CLAUDE.md` princípio 6,
reforçado aqui porque é regra de segurança, não só de estilo.
