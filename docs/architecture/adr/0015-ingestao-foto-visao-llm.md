# ADR-0015: Ingestão de documento por foto (mobile) via visão do LLM, sem OCR separado

Status: Aceita (confirmada pelo usuário em 2026-08-09, ao iniciar a fatia `document-service`)
Data: 2026-08-06

## Contexto

O app mobile precisa aceitar documento (fatura, extrato, boleto) enviado por
**foto**, não só PDF/CSV (pedido do usuário). Duas abordagens possíveis: (a)
pipeline de OCR tradicional (ex: Tesseract) que transforma a foto em texto,
reaproveitando o mesmo agente de parsing por texto já desenhado pra
fatura/extrato; (b) enviar a foto direto pra um LLM com capacidade de visão
(multimodal — ex: GPT-4o da OpenAI, ou um modelo como `llava`/`bakllava` via
Ollama), deixando o próprio modelo extrair os campos estruturados da imagem,
sem etapa de OCR separada.

## Decisão

Opção (b). Estender a porta `LlmProvider` (ADR-0002) com suporte a entrada de
imagem (ex: `chat(ChatRequest)` passa a aceitar um anexo de imagem opcional,
em vez de criar um método novo separado) — o agente de parsing de documento
(`ai-strategy.md` seção 4) usa isso quando a origem é uma foto, e o caminho
de texto puro (PDF/CSV, linha digitável do boleto) continua sem precisar de
visão.

## Consequências

- Menos uma dependência de infraestrutura (não precisa instalar/manter
  Tesseract ou similar) — mas cria uma exigência nova sobre `LlmProvider`:
  **o provedor escolhido precisa suportar visão**. OpenAI (GPT-4o) suporta
  bem; qualidade de modelos de visão no Ollama é mais variável e depende de
  hardware local (modelo multimodal costuma ser mais pesado que um modelo
  só-texto).
- Se o usuário tiver configurado só um provedor sem suporte a visão, upload
  de foto fica indisponível com mensagem clara — mesmo padrão de degradação
  já usado pra IA em geral (PRD 3.4).
- Foto de documento financeiro é dado sensível — vale o mesmo cuidado de
  privacidade já registrado pra texto (PRD seção 4): se for OpenAI, a foto
  sai pra fora; se for Ollama local, não sai.
- **Confirmado 2026-08-09**: usuário optou pela visão do LLM (opção b),
  sem OCR tradicional. Primeiro provedor configurado é Ollama local — a
  qualidade do modelo de visão local (`llava`/`bakllava`) precisa ser
  validada na prática quando a fatia de ingestão por foto for
  implementada (planejada só depois da fatia de fatura PDF, primeira
  fatia vertical do `document-service`); se a qualidade for ruim demais
  em produção, revisitar com um ADR novo (ex: exigir OpenAI só pra
  visão, ou reconsiderar OCR).
