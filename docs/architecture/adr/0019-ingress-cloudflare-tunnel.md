# ADR-0019: Ingress via Cloudflare Tunnel

Status: Aceita — substitui o detalhe de reverse proxy de ADR-0016
Data: 2026-08-07

## Contexto

ADR-0016 tinha deixado em aberto "Traefik ou Nginx" como reverse proxy pra
expor o sistema, assumindo TLS via Let's Encrypt e porta publicada no host.
O levantamento real do servidor (`docs/architecture/deployment.md`) mostrou
que o ambiente já usa **Cloudflare Tunnel** (`cloudflared`) pra expor o
portfólio — nada escuta em `0.0.0.0`, o túnel é quem conecta pra fora, de
dentro pra fora (outbound), sem porta aberta no firewall. Nginx está
instalado no host mas o serviço está inativo — não é o mecanismo em uso.

## Decisão

Sistema de finanças usa o **mesmo mecanismo** já validado no ambiente:
Cloudflare Tunnel. Adiciona-se um "public hostname" novo no túnel já
configurado (painel Cloudflare Zero Trust do usuário, fora do
repositório), apontando pro container de entrada do sistema (o front-end
Next.js, que já assume o papel de BFF — ADR-0006). TLS termina no Cloudflare;
tráfego `cloudflared` → container é HTTP simples dentro da rede
Docker/host, sem certificado próprio pra gerenciar.

## Consequências

- **Nenhuma porta nova no ufw** — superfície de ataque do host não muda.
- Container(s) alcançados pelo túnel devem escutar em endereço interno
  (`127.0.0.1` ou nome de serviço na rede Docker), nunca `0.0.0.0` sem
  necessidade — mesmo padrão que o portfólio já segue.
- Configuração do hostname/roteamento do túnel é feita fora do
  repositório (painel Cloudflare) — documentar aqui que essa dependência
  operacional existe, sem guardar credencial nenhuma do Cloudflare no repo
  (token do túnel é segredo, ver `security.md`).
- Perdemos a conveniência de um Traefik com auto-discovery via labels de
  Docker Compose, mas ganhamos simplicidade: uma peça a menos rodando, sem
  gestão de certificado.
- Nginx local segue instalado mas não utilizado por este projeto — se for
  reativado por outro motivo no futuro, revisar se conflita com algo aqui.
- Se o `ai-service` ou outro serviço precisar de endpoint público próprio
  (fora do Next.js), decidir então se ganha hostname próprio no mesmo túnel
  ou se continua só acessível via BFF — não antecipar agora.
