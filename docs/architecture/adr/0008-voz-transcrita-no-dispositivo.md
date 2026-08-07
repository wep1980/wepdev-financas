# ADR-0008: Entrada de voz no mobile via reconhecimento de fala nativo do dispositivo

Status: Aceita
Data: 2026-08-06

## Contexto

O app mobile precisa aceitar comando falado além de escrito (PRD 3.5); o
front-end web só aceita texto. Duas formas de resolver a voz: (a) o app grava
áudio e envia pro back-end, que transcreve via algum provedor (ex: OpenAI
Whisper API, ou um modelo local); (b) o app usa a API de reconhecimento de
fala nativa do próprio SO (Android/iOS) pra transcrever localmente, e só
envia o texto resultante.

## Decisão

Opção (b). O app mobile transcreve localmente e envia texto pro
`ai-service` — o mesmo pipeline usado pra comando escrito, sem nenhuma
mudança no back-end pra suportar voz.

## Consequências

- `LlmProvider`/`ai-service` (ADR-0002) não precisam lidar com áudio, upload
  de arquivo de voz, nem escolha de provedor de STT — menos superfície de
  integração, a porta continua só com `chat`/`embed`.
- Áudio da voz do usuário nunca trafega nem é armazenado no back-end — ganho
  direto de privacidade (PRD seção 4), especialmente relevante já que o
  conteúdo é sobre finanças pessoais.
- Qualidade da transcrição depende do reconhecimento de fala do próprio SO
  (varia entre Android/iOS e entre dispositivos/idiomas) — fora do controle
  do back-end.
- Se a qualidade da transcrição nativa for um problema real de UX no uso
  real, revisitar com um ADR novo (ex: STT no servidor via Whisper) — não
  antecipar essa complexidade agora.
