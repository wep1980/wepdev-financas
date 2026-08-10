# ADR-0024: Upload de documento é assíncrono (polling), não síncrono

Status: Aceita
Data: 2026-08-09

## Contexto

A spec original de `document-service.yaml` desenhava `POST /documentos`
como síncrono: a resposta HTTP já vinha com status `AGUARDANDO_CONFIRMACAO`
e a lista de lançamentos extraídos, tudo dentro da mesma requisição
(upload → extração de texto → chamada ao LLM → parsing → resposta).

Validação com uma fatura real (Santander, 2026-08-09, ver `docs/tasks.md`
fatia 3 item 5) mostrou que isso não é viável: mesmo depois de reduzir o
prompt enviado ao LLM de ~14KB pra ~5KB (recorte determinístico da seção
certa antes de montar o prompt), a extração de ~40 lançamentos via
`llama3.1` local rodando em CPU levou **vários minutos**, não segundos.
Uma requisição HTTP síncrona de vários minutos é uma experiência ruim (e
arriscada — timeout de proxy/load balancer, cliente mobile em rede
instável) pra uma operação que só vai crescer em fatura maior ou hardware
mais fraco.

## Decisão

`POST /documentos` volta a ser assíncrono desde o início: aceita o
arquivo, valida sincronamente (arquivo presente, tipo válido), persiste o
`DocumentoImportado` com status `RECEBIDO` e responde **202 Accepted**
imediatamente — sem lançamento nenhum ainda. A extração de verdade (PDFBox
+ `AgenteExtracaoFaturaService`) roda em background; o cliente (web/mobile)
sonda `GET /documentos/{id}` até o status sair de `PROCESSANDO` (vira
`AGUARDANDO_CONFIRMACAO` com lançamentos, ou `ERRO_PROCESSAMENTO` com
`mensagemErro`).

O domínio (`DocumentoImportado`, ver `docs/tasks.md` fatia 3 item 2) já
modelava esse ciclo `RECEBIDO → PROCESSANDO → AGUARDANDO_CONFIRMACAO/
ERRO_PROCESSAMENTO` desde a primeira versão — a mudança é só na camada
REST (o que a resposta HTTP imediata contém), não no domínio.

## Consequências

- Front-end (web/mobile) precisa implementar polling (ex: intervalo
  crescente, ou WebSocket/SSE numa fatia futura se a UX exigir algo mais
  ágil — não necessário pro MVP).
- Front-end mostra um estado de "processando" pro usuário depois do
  upload, em vez de já cair direto na tela de confirmação.
- Sem mudança na estrutura assíncrona do resto do fluxo (confirmação →
  Kafka → transaction-service já era assíncrono via evento).
- Quando o provedor de LLM configurado for mais rápido (ex: OpenAI, ou
  Ollama com GPU), a latência cai bastante — mas a API continua assíncrona
  de qualquer forma, não vale a pena ter dois modos (síncrono pra provedor
  rápido, assíncrono pro lento) só pra essa diferença.
