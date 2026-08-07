# ADR-0011: Push notification via Firebase Cloud Messaging (FCM)

Status: Aceita
Data: 2026-08-06

## Contexto

Alerta de vencimento (PRD 3.6) precisa de um canal push com som no app
mobile (React Native). Precisa funcionar em Android e iOS a partir de um
único back-end.

## Decisão

Firebase Cloud Messaging (FCM) — padrão de mercado pra push em apps React
Native (cobre Android nativamente; iOS via APNs por trás do FCM,
transparente pro back-end), com boas libs de integração
(`@react-native-firebase/messaging` ou Expo Notifications). App mobile
registra o device token no `notification-service`
(`POST /preferencias-notificacao/dispositivos`, ver
`docs/specs/notification-service.yaml`); `notification-service` guarda o
token e usa o SDK/API do FCM pra disparar a notificação (som fica a cargo do
payload da notificação, tratado pelo SO).

## Consequências

- Depende de conta Google/Firebase configurada no projeto — mais uma
  credencial externa a gerenciar (chave de service account do Firebase),
  guardada como segredo (Vault, nunca hardcoded, mesma regra de
  `CLAUDE.md`).
- Token de dispositivo pode expirar/mudar — `notification-service` precisa
  tratar falha de envio por token inválido (remover/atualizar registro), não
  só logar erro.
- Não cobre o caso do app fechado/desinstalado silenciosamente sem o
  back-end saber — aceitável, FCM retorna erro nesse caso e o token é limpo.
