<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Protocolos do aplicativo

O aplicativo consome APIs e não publica um OpenAPI HTTP próprio, e a sua única entrada externa é o callback registrado no manifest, já que os contratos de backend pertencem aos serviços.

## Callback e identidade

O redirect vem de `BuildConfig.AUTH_REDIRECT_URI`. O manifest define esquema por variante, host `auth` e caminho `/callback`. A Activity recebe `code` e `state`, remove a URI do Intent e encaminha a troca. O state deve coincidir com a pendência guardada, consumida uma vez.

A sequência REST usa `POST /auth/mobile/transactions`, `POST /auth/mobile/token`, `POST /auth/refresh` e `POST /auth/sessions/current/logout`, com a autorização feita no navegador e sem que o app solicite chave de serviço.

## Contratos consumidos

| Grupo | Uso |
|---|---|
| `api/projects` e sub-recursos | Consulta e criação de projeto, membros, atividades, histórico e fotos |
| Perfil e conexões no cadastro central | Dados da pessoa e das conexões |
| `messaging/conversations` | Lista, conversa direta e estado pessoal |
| `messaging/messages` | Histórico, envio, edição e exclusão |
| `messaging/updates` | Catch-up por cursor e época |
| `v1/address/geocode` | Endereço quando o flavor configura o destino |

Os clientes estão em [AuthApi](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/core/auth/AuthApi.kt), [ProjectApi](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/project/data/ProjectApi.kt), [ProfileApi](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/profile/data/ProfileApi.kt), [MessengerApi](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/communication/data/MessengerApi.kt) e [RegistryApi](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/discovery/data/RegistryApi.kt).

## Tempo real

A URL REST de mensagens é convertida em WebSocket no caminho `/messaging/ws`, e o Krossbow envia o Bearer no CONNECT com heartbeat de 10 segundos. Os eventos privados chegam por `/topic/users.{userId}.events`, e o cliente acompanha o ciclo de vida, desconectando ao ir para segundo plano.

A presença usa `/topic/conversations.{conversationId}`, e a digitação envia `/app/typing.started` e `/app/typing.stopped`. O transporte reconecta com espera crescente de 1 a 30 segundos, e uma falha identificada como de autenticação encerra a tentativa, sem repetição indefinida.

## Erros

O cliente categoriza 400 e 422 como validação, 401 como sessão, 403 como permissão, 404 como ausência, 409 como conflito, 429 como limite e 5xx como servidor. Campos de validação vêm de `errors[].field`. O contrato dos servidores está em [erros HTTP](https://github.com/Solierrr/docs-warehouse/blob/main/docs/standards/ERRORS.md).

A categoria da falha define o tratamento da fila, com validação, permissão ou destino ausente marcando a mensagem como falha, sessão inválida pausando a fila e as demais falhas permitindo uma nova tentativa depois. O cliente verifica o histórico recente antes de reenviar, mas isso não demonstra envio único no servidor. <!-- PENDENTE: 5 -->