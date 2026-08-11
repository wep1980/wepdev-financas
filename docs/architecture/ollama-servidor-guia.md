# Guia: usando o Ollama compartilhado do servidor

> Tutorial prático — como **qualquer sistema** rodando no servidor (ou na
> mesma rede local) usa o Ollama com GPU dedicada. Decisão de arquitetura
> e o histórico completo de como chegamos aqui ficam em
> [ADR-0029](adr/0029-ollama-servidor-gpu-dedicada.md) e
> `docs/historico.md` (2026-08-11) — este documento é só o "como usar",
> não repete o "porquê".

## 1. O que é

Uma instância única do [Ollama](https://ollama.com) rodando no servidor
de produção, com GPU dedicada (RTX 5070 Ti, 16GB VRAM). Serve modelo de
linguagem (LLM) via API HTTP simples pra qualquer sistema que precisar,
sem cada projeto ter que rodar/manter o seu próprio Ollama.

**Não é exclusiva do `wepdev-financas`** — é infraestrutura compartilhada
do servidor, do mesmo jeito que o Postgres/Umami/Portainer já hospedados
lá servem múltiplos projetos.

## 2. Como conectar

```
http://<IP_DO_SERVIDOR>:11434
```

Substitua `<IP_DO_SERVIDOR>` pelo endereço real da rede local — **nunca
commitar esse valor em nenhum arquivo versionado** (mesma regra de
`docs/architecture/security.md`: IP/hostname real só em `.env`
(gitignored) ou variável de ambiente). No `wepdev-financas`, o valor vive
em `OLLAMA_SERVER_URL` (`.env`, raiz do repo).

Teste rápido de que está no ar:

```bash
curl http://<IP_DO_SERVIDOR>:11434/api/tags
```

Resposta esperada: JSON listando os modelos instalados.

## 3. Modelos disponíveis

| Modelo | Tamanho | Uso típico |
|---|---|---|
| `llama3.1` | 4.9GB (Q4_K_M) | Uso geral — chat, extração de texto estruturado |

Pra ver a lista atualizada: `GET /api/tags`. Pra adicionar um modelo
novo (precisa de acesso SSH ao servidor):

```bash
docker exec ollama ollama pull <nome-do-modelo>
```

**Antes de adicionar um modelo novo, pense em VRAM** (seção 5) — cada
modelo carregado consome memória da GPU compartilhada por todo mundo que
usa essa instância.

## 4. API — exemplos práticos

### Geração simples (`/api/generate`)

```bash
curl http://<IP_DO_SERVIDOR>:11434/api/generate -d '{
  "model": "llama3.1",
  "prompt": "Sua pergunta aqui",
  "stream": false
}'
```

Resposta inclui `response` (texto gerado) e métricas úteis:
`eval_count` (tokens gerados), `eval_duration` (tempo em nanossegundos)
— dá pra calcular tokens/segundo real: `eval_count / (eval_duration /
1e9)`.

### Chat com histórico (`/api/chat`)

```bash
curl http://<IP_DO_SERVIDOR>:11434/api/chat -d '{
  "model": "llama3.1",
  "messages": [{"role": "user", "content": "Sua pergunta aqui"}],
  "stream": false
}'
```

### Embeddings (`/api/embeddings`)

Usado pelo `ai-service` do `wepdev-financas` pra indexação RAG (Qdrant).
Ver `ai-strategy.md` seção 2 pro contexto de uso.

`stream: false` é importante pra clientes que só querem a resposta
pronta (não incremental) — todos os exemplos acima já usam.

## 5. Concorrência e capacidade — leia antes de integrar um sistema novo

Configuração atual: `OLLAMA_NUM_PARALLEL=4` (até 4 gerações simultâneas).
Medido na prática (2026-08-11, ver ADR-0029):

- **Geração isolada**: ~127 tokens/segundo.
- **Duas gerações concorrentes** (uma grande ~3000 tokens de prompt +
  uma curta): ambas terminam em ~4 segundos, sem fila.

**Isso não é capacidade infinita.** Duas regras práticas:

1. **Um prompt muito grande (milhares de tokens) ainda pode atrasar
   outras gerações simultâneas**, mesmo com slots paralelos — achado
   real documentado na ADR-0029 (o `llama.cpp` processa os slots dentro
   do mesmo laço de lote; um prompt gigante domina esse laço). Se seu
   sistema manda prompts grandes com frequência, considere: dividir em
   pedaços menores, ou aceitar que picos de latência podem acontecer
   quando coincidir com outro consumidor pesado.
2. **VRAM é compartilhada entre todo mundo.** 16GB totais, modelo
   `llama3.1` usa ~5-7GB com os slots atuais. Antes de outro sistema
   carregar um segundo modelo grande simultaneamente, verifique memória
   livre:

   ```bash
   curl http://<IP_DO_SERVIDOR>:11434/api/ps
   ```

   (mostra modelos carregados e VRAM em uso). Se a soma dos modelos
   ultrapassar os 16GB, o Ollama descarrega o menos usado recentemente
   pra abrir espaço — pode causar lentidão inesperada pro sistema que
   tinha o modelo descarregado.

## 6. Timeout recomendado do lado do cliente

Depende do tipo de chamada. Referência real usada no `wepdev-financas`
(`application.properties` de cada serviço):

| Tipo de chamada | Timeout recomendado | Por quê |
|---|---|---|
| Pergunta curta / classificação (poucas centenas de tokens) | **130s** | Cobre até uma eventual disputa de recurso sem cortar cedo demais |
| Extração de documento grande (milhares de tokens de prompt) | **600-610s** | Prompt processing de um documento grande já mediu minutos em cenário de contenção |

Configure o timeout do **cliente HTTP** (não só um valor "otimista")
sempre acima do que o cenário realista pode levar — subestimar já causou
timeout em produção neste projeto (ver `docs/historico.md`, 2026-08-11).

## 7. Segurança — regras que não têm exceção

- **A porta 11434 não tem autenticação própria** — qualquer coisa que
  alcançar a porta consegue usar a API. A única barreira é de rede.
- **Nunca exponha essa porta pra internet pública** — nem via
  Cloudflare Tunnel, nem port-forward, nem qualquer outro mecanismo. Uso
  é só dentro da rede local/privada do servidor.
- Se algum dia um sistema **fora** da rede local precisar desse Ollama
  (ex: um serviço rodando na nuvem), a solução é VPN ou um proxy com
  autenticação própria na frente — **não** abrir a porta diretamente.
  Isso ainda não foi decidido/implementado; se a necessidade aparecer,
  discutir antes de simplesmente expor a porta.
- Dado sensível no prompt (ex: descrição de transação financeira) não
  fica retido entre chamadas de sistemas diferentes — cada request ao
  Ollama é isolado (quem mantém histórico de conversa é responsabilidade
  de cada sistema cliente, não do Ollama).

## 8. Persistência e ciclo de vida

- Modelos baixados ficam no volume Docker `ollama-data` — sobrevivem a
  restart do container (`docker restart ollama`) e do host.
- O container tem `--restart unless-stopped` — volta sozinho se o
  servidor reiniciar, a menos que alguém tenha parado ele manualmente
  antes do desligamento.
- **Hoje o Ollama roda fora de qualquer `docker-compose.yml`** (só
  `docker run` direto no servidor) — não gerenciado por nenhum projeto
  específico. Isso é uma pendência conhecida (ver ADR-0029, seção
  "Consequências"): quando o `wepdev-financas` fizer deploy completo
  nesse servidor (roadmap fatia 9), o Ollama deve ser incorporado ao
  compose de produção do projeto.

## 9. Se algo não funcionar

```bash
# O container está rodando?
docker ps --filter name=ollama

# Logs recentes
docker logs ollama --tail 50

# GPU sendo usada de verdade?
nvidia-smi

# Modelo carregado agora, memória em uso
curl http://<IP_DO_SERVIDOR>:11434/api/ps
```

Se a GPU não aparecer em `nvidia-smi` ou o container não subir com
`--gpus all`, o driver/NVIDIA Container Toolkit pode ter quebrado —
ver ADR-0029 seção "Decisão" pro passo a passo original de instalação.

## 10. Coordenação entre sistemas

Como é recurso compartilhado, **antes de conectar um sistema novo**:

1. Confira a capacidade atual (`/api/ps`, seção 5) — não assuma que tem
   VRAM/throughput sobrando sem checar.
2. Se o uso novo for pesado (prompts grandes, alto volume), considere
   avisar/documentar aqui (nova linha na tabela da seção 3, ou uma seção
   nova) pra quem mexer depois saber que existe outro consumidor.
3. Nunca mude `OLLAMA_NUM_PARALLEL` ou pare o container sem considerar
   que outros sistemas podem estar usando — isso não é mais uma decisão
   local de um projeto só.
