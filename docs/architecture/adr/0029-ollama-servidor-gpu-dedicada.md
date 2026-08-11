# ADR-0029: Ollama passa a rodar no servidor de produção, numa GPU dedicada

Status: Aceita
Data: 2026-08-11

> Como usar o Ollama compartilhado do servidor (API, timeouts, limites de
> concorrência, regras de segurança) está em
> [`docs/architecture/ollama-servidor-guia.md`](../ollama-servidor-guia.md)
> — este documento é só a decisão e o histórico, não o "como usar".

## Contexto

Testando o sistema de verdade pelo navegador (fatura Santander real +
pergunta no chat), confirmamos empiricamente que o Ollama local — CPU
only, container `ollama` do `docker-compose.yml`, mesma máquina que roda
todo o resto — não aguenta as três coisas que o produto promete ao
mesmo tempo: extrair uma fatura grande, responder o chat, e (por
extensão) atender mais de um usuário simultâneo (ver `docs/historico.md`,
2026-08-11).

Achados concretos que levaram a essa decisão:

- Uma única extração de fatura já usava ~6 núcleos a ~4 tokens/segundo —
  bem lento mesmo sozinha.
- Por padrão, o Ollama só processa **uma geração por vez** (fila
  serializada) — uma pergunta no chat enviada enquanto uma fatura
  processava ficou "na fila" atrás dela.
- Testado `OLLAMA_NUM_PARALLEL=2` (permite 2 gerações concorrentes,
  cada slot com seu próprio contexto): ajudou só parcialmente. A
  primeira chamada do `ai-service` rodou livre, mas a segunda colidiu
  com o prompt processing do documento (3139 tokens, ~129s só nessa
  fase) e ficou esfaimada mesmo tendo slot dedicado — achado técnico
  real: o `llama.cpp`/Ollama processa os slots dentro do MESMO laço de
  lote, e um prompt grande domina esse laço, atrasando a geração de
  outro slot mesmo ele "não estando na fila" formalmente. Resultado
  prático: chat e extração de fatura estouraram timeout as duas vezes
  que tentamos (chat 120s, fatura 600s — `ERRO_PROCESSAMENTO` nas duas
  tentativas de upload dessa sessão).
- RAM também é curta pra essa solução: só ~4GB livres na máquina rodando
  **só** este projeto (nada de outros projetos). O servidor de produção
  tem o mesmo perfil de hardware (`docs/architecture/deployment.md`) e
  **ainda por cima já hospeda outros projetos** (portfólio, Umami,
  Postgres, pgAdmin, Portainer, Watchtower) — rodar uma segunda
  instância completa do Ollama (mais um modelo de ~5-8GB carregado) não
  cabe com folga em nenhum dos dois ambientes.

Conclusão discutida com o usuário: em CPU sem GPU, não tem ajuste de
config que resolva isso de verdade — o teto é físico (throughput de
geração de token). O caminho real é hardware.

## Decisão

**Ollama passa a rodar fora do `docker-compose.yml` deste projeto,
direto no servidor de produção, com uma GPU dedicada (RTX 5070 Ti, 16GB
GDDR7, 896 GB/s de banda).**

Comparado com alternativas de GPU mais baratas (RTX 3060 12GB, RTX 2080
Ti 11GB, RTX 5060 Ti 16GB — pesquisadas e comparadas com o usuário antes
da decisão final), o usuário optou por usar a RTX 5070 Ti que já tinha
disponível numa outra máquina, em vez de comprar uma placa nova.

Passos executados nesta sessão (detalhe completo em `docs/historico.md`,
2026-08-11):

1. **Preparação do servidor**: acesso SSH dedicado pro Claude Code
   configurado (chave `ed25519` sem passphrase, restrita por IP de
   origem — ver inventário de credenciais em `security.md`), incluindo
   um achado real (`sshd_config` forçava só senha, corrigido). Servidor
   levantado por completo (CPU, RAM, disco, placa-mãe) antes de mexer em
   qualquer coisa física.
2. **Hardware**: disco de sistema (SSD NVMe, conectado via placa
   adaptadora PCIe) precisou ser realocado pro slot M.2 nativo da
   placa-mãe pra liberar o slot PCIe x16 pra GPU — verificado
   previamente que o boot usa UUID/LVM (não caminho físico de
   dispositivo), então a troca de slot não quebra o sistema. Confirmado
   na prática: reboot depois da troca, sistema subiu normal, todos os
   outros projetos (portfólio, Umami etc.) voltaram sozinhos.
3. **Driver + runtime**: `nvidia-driver-570-open` pedido, mas o
   resolvedor de dependência do `apt` instalou a versão mais nova
   disponível de fato (`580.173.02`, também suporta a arquitetura
   Blackwell da RTX 50 — não é regressão). `nouveau` desabilitado via
   blacklist do próprio pacote, precisou de reboot pra trocar de driver.
   NVIDIA Container Toolkit instalado e configurado (`nvidia-ctk runtime
   configure --runtime=docker`), validado com container `nvidia/cuda`
   real reconhecendo a placa.
4. **Acesso privilegiado**: usuário optou por dar `sudo` **irrestrito,
   sem senha** (`NOPASSWD: ALL`) pra chave do Claude Code, depois de eu
   apresentar o trade-off (escopo restrito a comandos específicos vs.
   total) — decisão dele, documentada em `security.md`.
5. **Ollama no servidor**: container `ollama/ollama` com `--gpus all` e
   `OLLAMA_NUM_PARALLEL=4` (16GB de VRAM dá folga — modelo usa ~5-7GB,
   sobra bastante pra vários slots de contexto), porta `11434` publicada
   na rede local. Testado e validado:
   - Velocidade real: **~127 tokens/segundo** (contra ~4 tokens/s na
     CPU — mais de 30x mais rápido).
   - Concorrência real: prompt grande (simulando fatura, ~3000 tokens)
     + pergunta curta enviados ao mesmo tempo — os dois terminaram em
     **~4 segundos cada**, sem fila, sem timeout. O problema que motivou
     esta ADR está resolvido de verdade, não só mitigado.
6. **`document-service`/`ai-service` reapontados**: `OLLAMA_BASE_URL`
   agora vem de `OLLAMA_SERVER_URL` (variável nova, só em `.env`
   local/gitignored — endereço de rede interna nunca em arquivo
   versionado, mesma regra de sempre). Serviço `ollama` removido do
   `docker-compose.yml` (container, porta, volume `ollama-data` — tudo
   fora). Validado de ponta a ponta: pergunta real via `ai-service`
   local batendo no Ollama do servidor, resposta em ~2,2s.

## Consequências

- **Positivo**: chat e extração de fatura deixam de competir entre si;
  velocidade de geração 30x maior; RAM da máquina de dev (e do servidor)
  liberada do peso de rodar um modelo de LLM localmente.
- **Negativo/trade-off assumido conscientemente**: `document-service` e
  `ai-service` (rodando localmente em dev) agora dependem de rede
  alcançar o servidor de produção — não funciona totalmente offline nem
  isolado da rede do usuário. Mitigado documentando no `.env.example`
  como voltar a apontar pra um Ollama local (`ollama/ollama` avulso,
  `http://localhost:11434`) se a rede não estiver disponível.
- **Segurança**: acesso SSH root irrestrito ao servidor de produção
  passou a existir pra uma chave automatizada (mesmo restrita por IP de
  origem) — trade-off aceito explicitamente pelo usuário em troca de
  menos fricção operacional. Porta do Ollama (`11434`) exposta na rede
  local do usuário, sem autenticação própria (comportamento padrão do
  Ollama) — aceitável no contexto de rede doméstica privada, mesmo
  critério já usado pra SSH (nunca exposto à internet pública).
- **Pendente**: deploy completo do `wepdev-financas` nesse mesmo
  servidor ainda não aconteceu (fatia 9 do roadmap, ainda `🔲
  Planejado`) — quando acontecer, esse Ollama avulso (hoje um `docker
  run` isolado, fora de qualquer `docker-compose.yml`) deve ser
  incorporado ao compose de produção do projeto, não deixado como
  configuração solta no servidor.
