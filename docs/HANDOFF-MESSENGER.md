# Handoff: integração do app com o api-messenger

Este arquivo é autossuficiente. Quem o lê (pessoa ou IA, em outro computador) não precisa de nenhum contexto anterior: tudo o que importa está aqui ou nos repositórios do GitHub.

- App: `Solierrr/mobile-app`, pacote `com.project.solaria_mobile` (Kotlin, Compose, Hilt, Retrofit com kotlinx.serialization, Krossbow STOMP, Room).
- Servidor: `Solierrr/api-messenger`, `main` no commit `4b32199`.
- O PR #61 foi integrado em `main` (`08ad3c3`). A continuação está em `feat/messenger-integration-completion`.
- Escopo ajustado pelo solicitante em 2026-11-02: chat, mídia, grupos e comunidades. Avaliações e NPS ficam fora desta continuação.
- O solicitante autorizou pular testes impedidos pelo ambiente. Não confundir testes escritos com testes executados.

## 1. Objetivo

Deixar o app integrado a **todas** as features e casos de uso do api-messenger. Quase tudo já tem código e testes de unidade; o que falta está na seção 5. **Nada disso passou por Gradle, KSP/Hilt, Room nem execução em aparelho**: o primeiro passo no novo computador é validar isso (seção 6).

Onde ler o contrato do servidor (o `openapi.yaml` está incompleto, não confie só nele):

| Fonte (no repositório `Solierrr/api-messenger`) | Conteúdo |
|---|---|
| `openapi.yaml` | Parte das rotas. Faltam `PATCH /conversations/{id}/state`, `PUT /conversations/{id}/manager`, `PATCH`/`DELETE /messages/{id}` e `POST /nps`, e vários códigos de erro. |
| `docs/API.md`, `docs/REALTIME.md`, `docs/ARCHITECTURE.md` | Tabela de erros, STOMP e arquitetura. |
| `flows/*.md` | Um fluxo por caso de uso: `SEND_MESSAGE`, `SEND_MEDIA`, `GROUP_MANAGEMENT`, `PROJECT_COMMUNITY`, `CATCH_UP`, `MESSAGE_EDIT_DELETE`, `DIRECT_CONVERSATION`, `ASSISTANT_MESSAGE`, `WEBSOCKET_CONNECTION`. |
| `src/main/java/com/solaria/messenger/core/contract/request/**` e `.../response/**` | DTOs reais de entrada e saída. É a verdade quando a documentação diverge. |
| `src/main/java/com/solaria/messenger/**/controller/*Controller.java` | Rotas e status HTTP. |

Para ler sem clonar: `gh api repos/Solierrr/api-messenger/contents/<caminho>?ref=4b32199 --jq .content | base64 -d`.

## 2. Contrato resumido

Base: `BuildConfig.MESSENGER_BASE_URL`, prefixo `/messaging`. Autenticação: `Authorization: Bearer <JWT>`. O `sub` do JWT é o `authId` (UUID) do usuário, o mesmo que o app usa para identificar pessoas. UUIDs devem ser comparados em minúsculas.

### 2.1 REST

| Área | Rota | Corpo / resposta | Estado no app |
|---|---|---|---|
| Conversas | `GET /conversations/me?archived=` | lista com `participantIds`, `managerUserId`, `unreadCount`, `lastMessage`, `muted`, `archived`, `syncVersion` | feito (normais e arquivadas) |
| | `GET /conversations/{id}` | `ConversationResponseDTO` (`conversationType` DIRECT, GROUP ou CHAT_BOT; `title` nulo em DIRECT) | feito |
| | `POST /conversations/direct` `{recipientId}` | 201 mesmo quando reaproveita | feito |
| | `POST /conversations/group` `{title<=120, participantIds}` | `{conversation, admittedParticipantIds, rejectedParticipants:[{userId, reasonCode}]}` | feito |
| | `POST /conversations/{id}/participants` `{userIds}` | `ConversationDto` com `rejectedParticipants` | feito |
| | `PUT /conversations/{id}/manager` `{successorUserId}` | só o gestor | feito |
| | `DELETE /conversations/{id}/participants/{userId}?successorUserId=` | 204; sair é remover a si mesmo | feito |
| | `PATCH /conversations/{id}/state` `{muted, archived, lastReadSequence}` | | feito |
| Mensagens | `POST /messages`, `PATCH`/`DELETE /messages/{id}`, `GET /messages/conversation/{id}?sinceSequence\|beforeSequence&limit` | `messageType` USER_TO_USER, USER_TO_GROUP ou USER_TO_CHATBOT; `content<=8000`; `clientMessageId` (UUID, sem deduplicação no servidor) | feito |
| Catch-up | `GET /updates?since&limit` | `{syncEpoch, cursor, hasMore, messages[changeVersion]}` | feito |
| Mídia | `POST /media` `{conversationId, type, mimeType, sizeBytes}` | `{mediaId, upload{url, method, headers}, uploadExpiresAt, maxBytes}` | feito |
| | `GET /media/{mediaId}/content` | 302 para link assinado (10 min); 410 `MEDIA_EXPIRED` após 7 dias | feito |
| | `GET /media?conversationId&limit&cursor` | lista o que ainda vale baixar | implementado no contrato e repositório; consumo no load continua opcional |
| Comunidade | `GET /communities/project/{projectId}` | `status` ACTIVE ou ARCHIVED; `members[{userId, role}]` | feito |
| | `GET /communities/me`, `GET /communities/{id}` | lista e leitura por id | `me` feito; por id não é usado |
| | `POST /communities` | só o demandante do projeto; 409 se já existe | feito |
| | `PATCH /communities/{id}` `{status}` | só OWNER e ADMIN | feito |
| | `POST /communities/{id}/members` `{userIds, role}` | ADMIN exige OWNER ou ADMIN; rebaixar usa `role:"MEMBER"` e exige OWNER | feito |
| | `DELETE /communities/{id}/members/{userId}` | **com corpo** `{successors:[{conversationId, successorUserId}]}`; 503 `REMOVAL_INCOMPLETE` = repetir o mesmo pedido | feito |
| | `POST /communities/{id}/conversations`, `GET .../conversations` | subgrupo; a listagem NÃO traz `status`, traz `managerUserId` | feito |
| Avaliações | `POST /ratings` `{evaluatedId (authId), typeRate PROFISSIONAL ou PRODUTO, rating 0.0 a 5.0, comment<=1000}` | ninguém avalia a si mesmo (422) | feito (UI só para PROFISSIONAL) |
| | `GET /ratings/evaluated/{id}`, `GET /ratings/evaluator/{id}`, `GET /ratings/{id}` | | feito, exceto por id |
| | `PATCH /ratings/{id}` `{rating, comment}`, `PATCH /ratings/{id}/status` `{status ACTIVE, EXCLUDED ou DOWN}` | excluir é `EXCLUDED` | feito |
| NPS | `POST /nps` `{conversationId, score 0..10, environment?}` | só conversa CHAT_BOT; 409 `DUPLICATE_RESOURCE` = já respondeu | feito |

Nomes de pessoas **não** vêm do messenger. O app usa o api-core: `GET {CORE_BASE_URL}/api/users/auth/{authId}/summary` devolve `{username, avatar}` (`ParticipantNames`, cache em memória por `authId`).

### 2.2 Erros (ProblemDetail)

O corpo traz `errorCode`. O app o expõe em `ApiResult.Failure.errorCode` e converte em texto por `core/designsystem/components/FailureMessage.kt`. Códigos relevantes:

- 403 `UNAUTHORIZED_ACCESS` (regra de negócio) e `ACCESS_DENIED` (segurança): tratar os dois.
- 409 `SUCCESSOR_REQUIRED`, `SUCCESSOR_INELIGIBLE`, `GROUP_MANAGER_UNAVAILABLE`, `CONCURRENT_MODIFICATION`, `DUPLICATE_RESOURCE`, `MEMBERSHIP_CLEANUP_REQUIRED`.
- 410 `MEDIA_EXPIRED`; 422 `MEDIA_MISMATCH`, `MEDIA_NOT_UPLOADED`, `BUSINESS_RULE_VIOLATION`.
- 503 `REMOVAL_INCOMPLETE` (repetir), `DEPENDENCY_UNAVAILABLE`, e todo `*_UNCERTAIN` (consultar antes de repetir).
- Erros que o Spring gera sozinho (UUID inválido, 404/405/415) saem **sem** `errorCode`: tratar por status.

### 2.3 Tempo real (STOMP em `/messaging/ws`, token no CONNECT)

- O servidor só aceita `SUBSCRIBE` em `/topic/users.{uuid}.events`, `.acks` e `.errors`. Qualquer outro tópico é recusado com `SUBSCRIBE_NOT_ALLOWED`. O app usa um canal só por conta (`StompChatRealtime`, singleton).
- Comandos: `/app/messages.send`, `/app/typing.started`, `/app/typing.stopped`, `/app/presence.ping`.
- Envelope: `{type, conversationId, eventId, serverTimestamp, payload}`. Tipos: `MESSAGE_CREATED`, `MESSAGE_EDITED`, `MESSAGE_DELETED`, `CONVERSATION_UPDATED` (mesma projeção da lista), `CONVERSATION_REMOVED` (vem sem `conversationId` no envelope; o id está em `payload.id`), `TYPING_STARTED`, `TYPING_STOPPED`, `USER_ONLINE`, `USER_OFFLINE`, `PRESENCE_SNAPSHOT`.
- Regras: janela de **30 minutos** para editar e excluir mensagem; só o gestor adiciona, remove e passa a gestão; o gestor que sai com outros no grupo precisa de sucessor; papéis de comunidade OWNER, ADMIN e MEMBER (só o dono rebaixa ou remove administrador; todo integrante que não é dono pode sair); comunidade arquivada é só leitura (envio recusado) e conversa com o assistente não aceita anexo.
- Mídia: o arquivo vai direto ao bucket pela URL assinada, **sem** `Authorization` (o app usa um `OkHttpClient` próprio no `AndroidMediaGateway`). Depois a mensagem cita o `mediaId`. Tetos: imagem 10 MB, áudio 16 MB, vídeo 64 MB, arquivo 25 MB. O download é `GET /media/{id}/content` autenticado; o OkHttp segue o 302 e, por ser interceptor de aplicação, o token não vai ao host do bucket. A mídia dura 7 dias no servidor; localização não usa bucket.

## 3. Arquitetura do app (para se orientar)

Um módulo Gradle `:app`, organizado por feature. Leia `docs/ARCHITECTURE.md`. Raiz Kotlin: `app/src/main/kotlin/com/project/solaria_mobile/`.

- Camadas: `Screen` (Compose, sem NavController) <- `ViewModel` (`@HiltViewModel`, `StateFlow<UiState<T>>`) <- `Repository` (interface em `domain/`, `Remote*Repository` em `data/`, ligado por `@Binds`) <- `*Api` (Retrofit). Falhas são `ApiResult` (`Success` ou `Failure(kind, invalidFields, errorCode)`), executadas por `ApiCallExecutor`.
- Navegação: `presentation/navigation/AppDestination.kt` (destinos serializáveis) e `AppNavHost.kt`.
- Design system: `core/designsystem/components/` (`ScreenTitle`, `ListRow`, `ActionRow`, `BottomSheet`, `AppDialog`, `AppMenu`, `PrimaryButton`, `FormField`, `StateNotice`, `UiStateHost`). Reaproveite; não crie widgets novos sem necessidade.
- Strings: `app/src/main/res/values/strings.xml`, `values-en` e `values-es`. Toda string nova entra nos três.
- Modo de demonstração: `core/mock/MockApiInterceptor.kt` (e `MockChatSeed.kt`) intercepta as chamadas quando o modo está ligado. **Toda rota nova precisa de resposta lá**, senão a demonstração quebra.
- Comentários e KDoc em português, no tom do código ao redor; identificadores em inglês.

## 4. O que já está feito neste PR

Commit `89afb17` `fix: align the messenger integration with the server contract`
- `CommunityGroupDto` sem `status` (o subgrupo vale o status da comunidade), `reasonCode` nos recusados, nomes de participantes via api-core (`ParticipantNames`), cache do `messageType` por conversa, `ConversationRemoved` vira `removed`.

Commit `b0c1d33` `feat: receive every conversation's events through the user channel`
- Canal único `.events`, presença global, lista de conversas ao vivo (`applyLive`), recuperação por `/messaging/updates`, leitura com debounce.

Commit `aeb34e2` `feat: cover groups, communities, media, ratings and NPS of the messenger`
- **Grupo avulso**: criar (menu da lista de conversas -> "Novo grupo", `PeoplePickerSheet` com as conexões), detalhes (`GroupDetailsScreen`, aberto pelo cabeçalho do chat), adicionar, remover, passar a gestão, sair com escolha de sucessor.
- **Arquivadas** (`ArchivedConversations.kt`) com desarquivar.
- **Comunidades**: lista (`Communities.kt`), arquivar e reativar, tornar e retirar administrador, remover integrante com escolha de sucessor por subgrupo, sair da comunidade (`ProjectCommunitySection.kt`, `RemoteProjectCommunityRepository`).
- **Mídia real**: envio (foto e documento; áudio e vídeo viajam como `AUDIO` e `VIDEO` quando o mime é de áudio ou vídeo), download (foto baixa sozinha, arquivo ao tocar), "mídia indisponível" em 410 ou prazo vencido. Código em `data/MediaGateway.kt`, `RemoteCommunicationRepository`, `ChatViewModel`, `ChatMessageViews`.
- **Avaliações** (`feature/rating/`): média e comentários no perfil de outra pessoa, avaliar, editar, excluir, e "Minhas avaliações" no perfil próprio.
- **NPS** ao sair de uma sessão do assistente (`NpsSheet.kt`, `ChatViewModel.submitNps`).
- Nome da conversa direta que chega ao vivo (`ConversationListViewModel.resolveDirectNames`).
- `errorCode` em `ApiResult.Failure` e mensagens por código.
- Mock de todas as rotas novas.
- Testes: 163 de unidade passando fora do Gradle (ver seção 6).

## 5. O que falta (checklist)

Cada item: arquivos, rotas, testes e critério de pronto.

### 5.0 Validar o que existe (primeiro)
- [ ] Rodar `./gradlew :app:testDebugUnitTest :app:assembleDebug` e corrigir o que o Gradle apontar. **Nunca foram validados**: o grafo Hilt/KSP (em especial `ParticipantNames`, que tem construtor primário `internal` e um `@Inject` secundário, e `MediaModule`/`RatingModule`/`RatingApi` em `di/ApiModule.kt`), o Room, `AppNavHost.kt` (os destinos novos `GroupDetails`, `ArchivedChats`, `Communities`, `MyRatings` foram escritos mas o arquivo não foi compilado), os recursos `res/` e o manifest.
- [ ] Rodar o app em aparelho/emulador com o **modo de demonstração** e passar pelos fluxos novos (grupo, arquivadas, comunidades, foto, avaliação, NPS).
- [ ] Rodar contra o messenger real (QA) e conferir principalmente: upload no bucket (cabeçalhos e `Content-Type` exatos), download (302), `DELETE` de comunidade com corpo (alguns proxies descartam o corpo).
- Critério: build verde, testes verdes, fluxos acima funcionando.

### 5.1 Mídia: o que ficou de fora
- [x] **Gravador de áudio e vídeo**: hoje áudio e vídeo só saem como arquivo escolhido no seletor. Criar origem "Gravar áudio" em `ChatAttachments.kt` (`AttachmentSheet`), com permissão de microfone, produzindo `MessageAttachment.File` com mime `audio/*` e `sizeBytes` real. Mostrar player simples no `FileCard` (`ChatMessageViews.kt`).
- [x] `GET /media?conversationId&limit&cursor` para listar o que ainda vale baixar numa sessão nova (rota em `MessengerApi.kt`, método em `CommunicationRepository`, uso opcional no `ChatViewModel.load`).
- [x] Largura e altura da imagem (`width`/`height` no `AttachmentDto`) no envio, para o servidor guardar e a bolha reservar o espaço.
- [x] Teste: `AndroidMediaGateway` (upload sem `Authorization`, método e cabeçalhos do alvo) com `MockWebServer`.

### 5.2 Grupo avulso
- [x] Mostrar na tela o aviso de convidados recusados ao criar grupo (`NewGroup.Created.rejected` já existe; a lista abre direto o chat e descarta o número). Arquivo: `ConversationsScreen.kt`.
- [x] Mostrar o motivo (`reasonCode`: `USER_NOT_FOUND`, `USER_INACTIVE`, `NOT_COMMUNITY_MEMBER`) em vez de só a contagem.
- [x] Foto dos participantes (hoje só iniciais): usar `ParticipantNames` estendido para devolver também `avatar` do summary.

### 5.3 Comunidades
- [x] Tela de uma comunidade (hoje a gestão fica na seção do projeto). Opcional: `GET /communities/{id}`.
- [x] Comunidade arquivada é só leitura no chat do subgrupo: usar `communityStatus` da projeção da lista (`ConversationListItemResponseDTO`) para trocar o campo de texto por um aviso. Hoje o envio falha com erro genérico. Arquivos: `MessengerApi.kt` (campo `communityStatus` em `ConversationDto`), `CommunicationModels.kt`, `ChatScreen.kt`.
- [x] Remoção em lote de sucessores quando vários subgrupos: teste Compose escrito para exigir uma escolha em cada um dos dois subgrupos; execução no aparelho ainda pendente.

### 5.4 Avaliações (fora do escopo por orientação do solicitante)
- [ ] UI para `PRODUTO`. O repositório e a API já aceitam `RatingType.PRODUTO`; falta uma tela de produto onde encaixar `ProfileRatingsSection` (o app ainda não tem tela de produto).
- [ ] Mostrar todas as avaliações (hoje o perfil mostra a média e os 3 comentários mais recentes): tela "Todas as avaliações" com paginação local.
- [ ] Mostrar o nome de quem avaliou nos comentários (resolver `evaluatorId` com `ParticipantNames`).

### 5.5 NPS (fora do escopo por orientação do solicitante)
- [ ] Persistir "já respondi" entre execuções (hoje é um conjunto em memória, `NpsAnswers` em `ChatViewModel.kt`, e o 409 do servidor evita duplicar). Guardar em DataStore.
- [ ] Decisão de produto: quando perguntar (hoje: ao sair de uma sessão que já teve resposta do assistente).

### 5.6 Outros casos de uso do messenger e lacunas do mobile
- [x] **Push abre a conversa** (`SolariaMessagingService`): o `NEW_MESSAGE` traz o id da conversa; confirmar com o api-notify se o payload tem `notificationId` (sem ele o app descarta o push).
- [x] **Prazo de 30 min** para editar e excluir: esconder ou desativar as ações no menu da mensagem depois do prazo, com o motivo (hoje volta erro genérico). Arquivo: `ChatScreen.kt` (`ChatMessageItem`).
- [x] **Indicador de conexão** ("Reconectando...", "Sem conexão em tempo real") a partir de `RealtimeState`, no chat e na lista.
- [x] **Reconectar com token vencido**: `StompRealtimeClient` trata recusa de token como `AuthFailed` e não reconecta até o app voltar ao primeiro plano; renovar o token e tentar de novo.
- [ ] **Envio mais barato e seguro**: guardar o tipo da conversa na fila (`MessageOutbox`) para não buscar a conversa; conferir duplicidade pelo `clientMessageId` (o servidor não deduplica); limite de tentativas para `UNAUTHORIZED`; avaliar WorkManager para a fila sobreviver ao fechamento do app.
- [x] **Contador de não lidas** no atalho "Conversas" da Home (`ActionTile` não aceita selo).
- [x] `*_UNCERTAIN` (503): consulta estado de grupo/comunidade e mensagens por páginas antes de liberar repetição. Se a consulta falhar ou vier apenas do cache, bloqueia ações até confirmação; anexos levam `clientMessageId`.
- [ ] Conversa direta (`POST /conversations/direct`) devolve 201 também quando reaproveita; nada a fazer, só não presumir "criada".

### 5.7 Qualidade
- [ ] Testes de UI (Compose) das telas novas; hoje só há testes de ViewModel e de repositório.
- [ ] Revisar acessibilidade (descrições de conteúdo das estrelas e dos menus) e o tema escuro nas telas novas.
- [x] Atualizar `docs/ARCHITECTURE.md`, `docs/API.md` e `docs/STATUS.md` com o que mudou.

## Continuação em 2026-11-02

Os itens marcados na seção 5 têm implementação; não atestam execução em aparelho. Foram acrescentados testes de unidade para cursor de mídia, dimensões, upload assinado, push, prazo e recuperação de token; e testes Compose para comunidade arquivada/ativa e seleção de sucessores em dois subgrupos. Estes testes ainda não foram executados com sucesso nesta máquina.

A fila passa a guardar `conversationType` e `authAttempts` (Room 3, migração aditiva 2→3), limita recusas 401 a três tentativas e consulta páginas anteriores do histórico pelo `clientMessageId` antes de reenviar. Mensagens interrompidas em `SENDING` voltam como incertas. O envio continua no processo; WorkManager foi avaliado e permanece pendente, porque exige coordenar conta, ciclo de sessão e um único consumidor da fila. O servidor continua sem deduplicação: a consulta de histórico reduz duplicatas, sem garantir entrega única.

Operações incertas de grupo e comunidade relêem o estado antes de liberar nova ação. Se a releitura falhar, a gestão fica bloqueada até conferir o estado. A criação de grupo atualiza a lista para a pessoa conferir antes de tentar criar de novo.

Validação tentada:
- `gradlew.bat` falha com `&` no caminho da pasta; a execução direta do JAR evita o wrapper.
- `Unable to establish loopback connection` deixou de ocorrer com `-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp`. O socket local usava o TEMP longo do sandbox.
- Foi necessário `-Pandroid.overridePathCheck=true`, `ANDROID_HOME` apontando ao SDK local e seleção do JDK completo para evitar um JRE sem `jlink`.
- O projeto tem flavors: usar `:app:testQaDebugUnitTest :app:assembleQaDebug :app:lintQaDebug`, não `testDebugUnitTest` (ambíguo).
- A tentativa real de testes/build chegou ao KSP e falhou internamente: `Unexpected owner function: null` em `:app:kspQaDebugKotlin`. Não houve resultado de testes nem APK validado.
- A compilação Kotlin completa de QA passou com KSP desativado por um init script temporário em `build/skip-ksp.init.gradle`: `:app:compileQaDebugKotlin` retornou `BUILD SUCCESSFUL`. Isso cobre Compose, navegação e recursos Kotlin, mas não valida processadores Hilt/Room nem geração/instalação de APK.
- ADB não conseguiu iniciar seu daemon; fluxos em aparelho/emulador e QA real permanecem sem validação.

Não incluir ajustes locais em `gradle.properties` ou `.idea/`. Os recursos novos existem em português, inglês e espanhol. O tema do projeto só fornece a paleta clara; a revisão completa em tema escuro continua pendente.

## 6. Workflow

Pré-requisitos: Android Studio com JDK 17+ e Android SDK 36/37; `gh` autenticado.

```bash
git clone https://github.com/Solierrr/mobile-app.git && cd mobile-app
git fetch origin && git checkout fix/messenger-community-and-names   # ou a main, se o PR já foi mesclado
./gradlew :app:testDebugUnitTest          # testes de unidade (JVM)
./gradlew :app:assembleDebug              # compila o app (KSP, Hilt, Room, Compose)
./gradlew :app:lintDebug                  # lint
```

`make test`, `make build` e `make ci` chamam os mesmos comandos. Uma variante QA/prod muda `BuildConfig` (URLs, `ENVIRONMENT`); veja `app/build.gradle.kts`.

**Armadilhas conhecidas**
- No computador onde isto foi escrito o Gradle falhava com `IOException: Unable to establish loopback connection` (bug local de rede). Por isso os 163 testes foram compilados e rodados **fora do Gradle**, com `kotlin-compiler-embeddable` 2.4.20 e os jars do cache. Isso cobre ViewModels, repositórios e a maior parte das telas Compose (compila), mas **não** KSP/Hilt, Room, manifest nem `AppNavHost`. Se o Gradle funcionar no seu computador, ignore essa limitação e rode tudo por ele.
- O Bash do ambiente de quem escreveu recusava heredocs longos; não é um problema do repositório.
- `retrofit2` não aceita `@Body` em `@DELETE`; use `@HTTP(method = "DELETE", path = "...", hasBody = true)` (já feito em `CommunityApi.removeMember`). Um corpo de erro HTTP só pode ser lido uma vez; em testes crie uma `HttpException` nova a cada chamada.
- `MessageDto` em tempo real não traz nome do remetente; ele vem de `Conversation.participantNames` aplicado no `ChatViewModel`.
- O token do usuário nunca pode ir ao host do bucket: o upload usa um `OkHttpClient` sem os interceptores da API.

**Padrões e convenções**
- TDD: escreva o teste que falha, depois o código. Testes em `app/src/test/java/com/project/solaria_mobile/`, JUnit 4, `mockk`, `turbine`, `MainDispatcherRule`. Fakes em `.../feature/communication/data/FakeCommunicationRepository.kt` (use `CommunicationRepository by FakeCommunicationRepository()` para sobrescrever só o que precisa).
- Mudança mínima, no padrão do código ao redor. Estado de tela é `UiState<T>`; erros chegam como `ApiResult.Failure`.
- Todo texto visível é string resource nos três idiomas. Toda rota nova tem resposta no `MockApiInterceptor`.
- Commits conventional em inglês (`feat:`, `fix:`, `docs:`), corpo em português, **um commit por bloco de feature**, sem `Co-Authored-By` e sem rodapé de ferramenta. Não inclua `.idea/` nem `gradle.properties` local. PR para `main`; o merge é feito pelo dono do repositório.

## 7. Prompt pronto para a IA que vai terminar o trabalho

```text
Você vai terminar a integração do app Android Solierrr/mobile-app (Kotlin, Compose, Hilt, Retrofit com
kotlinx.serialization, Krossbow STOMP) com o servidor Solierrr/api-messenger (main 4b32199).

1. Clone Solierrr/mobile-app e faça checkout de fix/messenger-community-and-names (PR #61). Leia
   docs/HANDOFF-MESSENGER.md inteiro ANTES de qualquer coisa: ele traz o contrato, o que já está feito, o que
   falta (seção 5) e o workflow. Leia também docs/ARCHITECTURE.md.
2. O contrato do servidor está em Solierrr/api-messenger: openapi.yaml (incompleto), docs/API.md, docs/REALTIME.md,
   flows/*.md e src/main/java/com/solaria/messenger/core/contract/**. Use `gh api` para ler. Quando divergirem,
   vale o código em contract/** e os controllers.
3. Primeiro, valide o que existe: rode ./gradlew :app:testDebugUnitTest e ./gradlew :app:assembleDebug e corrija
   o que quebrar (nada disso foi validado no Gradle: KSP/Hilt, Room, AppNavHost e res/ são os pontos de risco).
   Só depois implemente os itens pendentes da seção 5, na ordem em que aparecem.
4. Use TDD (skill test-driven-development, se existir): escreva o teste que falha, implemente o mínimo, rode.
   Use systematic-debugging se algo falhar sem causa óbvia. Antes de dizer que terminou, rode a suíte inteira e
   o assembleDebug e cole o resultado (skill verification-before-completion).
5. Regras: mudança mínima no padrão do código ao redor; strings nos três idiomas (values, values-en, values-es);
   toda rota nova com resposta no MockApiInterceptor; comentários em português; commits conventional em inglês
   com corpo em português, um por bloco, SEM Co-Authored-By e SEM rodapé de ferramenta; não inclua .idea/ nem
   gradle.properties. Não faça merge.
6. Ao terminar cada bloco, marque o item na seção 5 do HANDOFF-MESSENGER.md. No fim, faça revisão do diff
   completo (skill requesting-code-review), corrija os achados, faça push e atualize o corpo do PR #61 com
   gh pr edit 61 --repo Solierrr/mobile-app --body-file <arquivo>, listando o que foi feito, os testes
   rodados (com números) e o que ficou sem validar.
```
