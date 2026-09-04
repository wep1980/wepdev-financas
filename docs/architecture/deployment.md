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

**Atualização 2026-08-11 — GPU dedicada instalada (ADR-0029)**: o
servidor ganhou uma placa **RTX 5070 Ti (16GB GDDR7)**, movida de outra
máquina do usuário, especificamente pra rodar o Ollama sem a contenção
que CPU-only causava entre chat e extração de fatura (detalhe completo
em ADR-0029 e `docs/historico.md`). Mudanças físicas/de sistema:
- Disco de sistema (SSD NVMe) realocado de uma placa adaptadora PCIe pro
  slot M.2 nativo da placa-mãe (**ASUS TUF B360M-PLUS GAMING/BR**, i7-8700,
  6 núcleos/12 threads) — liberou o slot PCIe x16 pra GPU. Boot
  confirmado usando UUID/LVM (não caminho físico), troca de slot não
  quebrou nada.
- Driver NVIDIA `580.173.02` (open kernel module) + NVIDIA Container
  Toolkit instalados — `docker run --gpus all` funcionando.
- Ollama roda como container avulso no servidor (`docker run`, fora de
  qualquer `docker-compose.yml` por ora — ver "Pendente" na ADR-0029),
  porta `11434` na rede local, `OLLAMA_NUM_PARALLEL=4`.
- Acesso SSH pro Claude Code (chave dedicada, `sudo` irrestrito sem
  senha) usado pra fazer toda essa configuração — ver inventário de
  credenciais em `security.md`.
- Como qualquer sistema (não só o `wepdev-financas`) usa esse Ollama:
  [`docs/architecture/ollama-servidor-guia.md`](ollama-servidor-guia.md).

## 3. Ingress e deploy — em implantação

O ingress continua via **Cloudflare Tunnel**, agora encaminhado ao Traefik
interno do K3s. O deploy dos serviços de aplicação passou a usar imagens no
GHCR + repositório GitOps + Argo CD (ADR-0030). Runner self-hosted e Kamal
foram superados. Nenhuma credencial do cluster é entregue ao GitHub Actions
e nenhuma porta nova precisa ser aberta no firewall.

## 4. Bancos de dados e serviços de apoio

Para este sistema, MySQL, MongoDB, Kafka KRaft e Qdrant rodam no K3s em
StatefulSets de uma réplica com volumes locais de retenção. Keycloak persiste
em banco MySQL; Ollama continua no host com GPU. Vault + External Secrets
Operator fazem a injeção de credenciais e backups cifrados seguem pela
Tailscale para a VPS Oracle. Recursos, tamanhos e procedimento de recuperação
estão definidos na ADR-0031.

## 5. Topologia

Detalhe da decisão de produção em ADR-0016. Diagrama de implantação em
`docs/architecture/diagrams.md` seção 5.

## 6. Como o deploy chega no servidor

GitHub Actions executa build, testes e scans em runners hospedados e publica
no GHCR somente as imagens dos serviços alterados. Cada imagem recebe uma tag
imutável com o SHA do commit. O pipeline propõe por pull request a atualização
dessas tags no repositório `servidor-gitops`; após revisão e merge, o Argo CD
detecta a mudança e sincroniza o K3s (ADR-0030).

O GitHub Actions não acessa o servidor nem recebe `kubeconfig`. O Argo CD tem
acesso somente leitura ao Git. Público chega pelo Cloudflare Tunnel ao Traefik
e então ao Ingress/Service Kubernetes. Deploy só progride depois dos gates do
CI e da validação do repositório GitOps.
