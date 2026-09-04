# ADR-0020: Deploy via runner self-hosted do GitHub Actions

Status: Superada por ADR-0030
Data: 2026-08-07

## Contexto

ADR-0018 assumiu que o job de CD do GitHub Actions conectaria via SSH
direto no servidor de produção. O levantamento real (`deployment.md`)
mostrou que o SSH do servidor (porta não-padrão) só aceita conexão vinda da
VPN WireGuard do usuário (`10.8.0.0/24`) — um runner hospedado pelo GitHub
roda fora dessa rede e não alcançaria de jeito nenhum. Abrir uma exceção de
firewall pra faixa de IP de runners do GitHub foi descartado (faixa grande e
variável, enfraqueceria o isolamento que o SSH-only-VPN já garante).

## Decisão

Instalar um **runner self-hosted do GitHub Actions** no próprio servidor de
produção — processo registrado no repositório GitHub que **puxa** job do
GitHub (conexão outbound, iniciada pelo servidor), nunca o contrário. O job
de deploy roda inteiramente nesse runner, local ao servidor:
`docker compose pull && docker compose up -d` executam como um step normal
de workflow, sem SSH nenhum envolvido.

Desenho recomendado do pipeline (refina ADR-0018): **CI** (build, teste,
scan de vulnerabilidade) continua em runner hospedado do GitHub — não
consome recurso do servidor de produção, mais isolado. Só o job de **CD**
(que precisa necessariamente rodar local pra aplicar o deploy) usa o runner
self-hosted.

## Consequências

- **Não precisa mais de chave SSH de deploy** como GitHub Actions Secret —
  o passo que antes seria "conectar via SSH" deixa de existir; atualiza o
  inventário de credenciais em `security.md` (remove a entrada de chave SSH
  de deploy).
- **Ressalva de segurança conhecida de runner self-hosted**: se o
  repositório algum dia aceitar PR de fora (hoje é uso pessoal, não aceita),
  um runner self-hosted pode virar vetor de execução de código arbitrário no
  host a partir de um PR malicioso. Regra: nunca habilitar "Allow fork PRs to
  use self-hosted runners" nas configurações do repositório. Se o projeto
  algum dia aceitar contribuição externa, revisitar esta decisão antes.
- Runner self-hosted consome recurso do servidor de produção quando roda um
  job — aceitável dado o recurso disponível (12 vCPU/15GB, `deployment.md`).
- Instalação/registro do runner é passo operacional manual (token de
  registro do GitHub, executado uma vez no servidor) — cai na fatia 9 do
  roadmap (deploy em produção), não bloqueia o trabalho atual.
- Cada imagem continua taggeada por commit no `ghcr.io` (ADR-0018) — o
  runner self-hosted só troca *onde* o `docker compose pull/up` roda, não
  muda a estratégia de rollback já registrada lá.
