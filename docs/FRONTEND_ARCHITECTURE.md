# Arquitetura do frontend Compose

O app usa um módulo Gradle (`:app`) e organiza o código por feature. Esta refatoração mantém as telas e os fluxos existentes. Os dados ainda são demonstrativos, sem conexão com backend ou persistência, exceto as conversas e as mensagens, que usam o messenger e o banco local (ver [Chat com cache local](#chat-com-cache-local)). O Figma não é alterado nesta etapa.

## Estrutura

```text
com/project/solaria_mobile/
├── MainActivity.kt                    # tema e NavHost
├── SolariaApplication.kt              # Hilt
├── core/designsystem/
│   ├── theme/                         # cores, tipografia, espaçamento e raios
│   └── components/                    # componentes compartilhados, por responsabilidade
├── presentation/navigation/
│   ├── AppDestination.kt              # destinos @Serializable
│   └── AppNavHost.kt                  # único mapeamento de destinos para rotas
└── feature/
    ├── home/
    ├── project/
    ├── communication/
    ├── discovery/
    └── profile/
        ├── data/                     # contrato + FakeRepository + binding Hilt
        ├── *ViewModel.kt              # UiState, eventos e transformações
        └── *Screen.kt                 # Route + Screen + componentes locais
```

Uma feature pode ter várias rotas. O nome do arquivo acompanha a tela, e pequenos componentes privados permanecem próximos da tela que os usa. Componentes públicos reutilizados entre features ficam em `core/designsystem/components`. Não se cria um arquivo para cada `Text`, `Row` ou detalhe de layout.

## Responsabilidades e fluxo

| Peça | Responsabilidade | Analogia aproximada no React |
| --- | --- | --- |
| `Route` | Obtém o ViewModel via Hilt, coleta `StateFlow` com `collectAsStateWithLifecycle()` e conecta eventos/navegação. | Container conectado a uma store. |
| `Screen` | Recebe estado imutável e callbacks; desenha a UI sem buscar nem mutar dados. | Componente controlado por props. |
| `ViewModel` | Expõe `UiState` somente-leitura, filtra listas, trata eventos e chama repositórios. | Store escopada à rota. |
| `Repository` | Contrato para dados e ações. A implementação fake usa `MutableStateFlow` em memória e é `@Singleton`. | Serviço/store de dados compartilhado. |
| Design system | Fonte de tokens e componentes reutilizáveis. | Tokens e biblioteca de UI. |

O fluxo é `interação → callback da Screen → ViewModel → Repository/StateFlow → UiState → recomposição`. Não passar o ViewModel para componentes de folha. Estado puramente visual, como bottom sheet aberta, pode ficar em `remember`; mensagens, busca, calendário e ações compartilhadas ficam fora da UI.

Cada repositório fake é uma fonte única de verdade por feature enquanto o processo do app vive. Salvar localização atualiza o mesmo estado observado por outras rotas, sem DataStore nem chamada HTTP. As conversas fogem dessa regra. O repositório delas lê o Room. `HomeViewModel` usa também `ProjectRepository` para os projetos recentes. Previews usam fixtures próprias no source set `debug`, não Hilt.

## Chat com cache local

A lista de conversas e o chat leem só do Room (`solaria.db`) e abrem sem internet, com o que o aparelho já baixou. A tela observa `Flow`s do banco. A rede e o WebSocket só escrevem nele. O servidor é a autoridade, então o banco pode ser apagado a qualquer momento sem perda, exceto a fila de envio.

```text
ChatViewModel / ConversationsViewModel
        ^ Flow (só leitura)
RemoteCommunicationRepository  (junta banco, fila de envio e nomes)
        ^
Room: messages | conversations | users | sync_state | pending_messages
        ^ escreve               ^ escreve          ^ escreve
 MessageSync (REST + eventos)   UserDirectory     MessageOutbox (envio)
        |                         |                  |
        +---- MessageStore -------+------------------+
```

| Tabela | Guarda | Quem escreve |
| --- | --- | --- |
| `messages` | Mensagens confirmadas das conversas humanas, com `changeVersion`. | `MessageStore`, chamado por `MessageSync` e `MessageOutbox`. |
| `conversations` | Lista, prévia da última mensagem, `syncEpoch`, `syncVersion`, `archived` e `accessLost`. | `MessageSync`. |
| `users` | Nome e avatar de quem aparece nas conversas. | `UserDirectory`. |
| `sync_state` | `syncEpoch` e `cursor` do catch-up. | `MessageSync`. |
| `pending_messages` | Mensagens que esperam envio. | `MessageOutbox`. |

O Room está na versão 2, com `fallbackToDestructiveMigration`. A troca de versão apaga o banco, inclusive a fila de envio. Não há migração porque não há dados em produção.

### Quando o banco sincroniza

`App.onCreate` chama `MessageSync.start()`. Com usuário logado, o motor assina `/topic/users.{me}.events` e roda uma sincronização completa quando o app abre, quando o WebSocket conecta, quando a internet volta ou quando a lista chama `refresh()` no botão "Tentar novamente". Pedidos repetidos se fundem, nunca há duas sincronizações ao mesmo tempo, e uma falha com internet repete depois de 30 s. Cada sincronização faz três coisas, em ordem.

1. Lê `GET /messaging/conversations/me` duas vezes, com `archived=false` e `archived=true`, e grava a lista. A conversa guardada que sumiu das duas respostas ganha `accessLost = true`. Se ela voltar, a marca sai.
2. Lê `GET /messaging/updates`. Sem cursor guardado, só guarda `syncEpoch` e `cursor`. Com cursor, lê `?since=` enquanto `hasMore` for verdadeiro e guarda o cursor novo. Se o `syncEpoch` mudar, apaga `messages` e `sync_state` e recomeça. A fila de envio fica.
3. Completa cada conversa humana, da mais recente para a mais antiga. Preenche buracos de `sequence`, traz o que veio depois da última mensagem e baixa o histórico para trás até a `sequence` 1. O progresso sai do próprio banco, então uma sincronização interrompida continua de onde parou.

Os eventos de `/topic/users.{me}.events` entram no banco assim que chegam. `MESSAGE_CREATED`, `MESSAGE_EDITED` e `MESSAGE_DELETED` gravam a mensagem. `CONVERSATION_UPDATED` grava a conversa e `CONVERSATION_REMOVED` marca `accessLost`. Se o evento deixar um buraco de `sequence`, o motor busca só o trecho que falta. O app não assina mais `/topic/conversations.{id}`.

### Regras que valem em todo caminho

- **Versão maior vence.** Histórico, catch-up, evento e confirmação de envio gravam pela mesma regra (`mergeMessage` e `mergeConversation`, em `core/database/SyncRules.kt`). Uma atualização com versão menor ou igual é ignorada, então reaplicar o que já chegou não muda nada e uma página antiga não desfaz uma edição.
- **A exclusão apaga o texto.** Uma mensagem com `deleted = true` perde o `content` no banco e na prévia da lista.
- **O cursor só anda pelo catch-up.** Evento ao vivo nunca o move. Um evento perdido numa queda volta no próximo catch-up.
- **Conversa sem acesso fica em somente leitura.** Ela continua na lista com o aviso "Você não participa mais", o chat abre sem campo de envio e a sincronização para de ler o histórico dela.
- **O chatbot não é gravado.** As mensagens de `CHAT_BOT` ficam só em memória (`MessageStore`) e o chat do chatbot exige internet para abrir.

### Nomes e avatares

`UserDirectory` chama `GET /api/users/auth/{authId}/summary` no api-core, grava a pessoa só se ela é nova ou mudou, e não repete a mesma pessoa antes de 30 s. A conversa direta usa o nome do outro participante e as bolhas de grupo mostram o nome de quem enviou. Sem internet, a tela usa o que está no banco. O avatar fica salvo, mas a tela ainda mostra só as iniciais.

### Logout e backup

Quando a sessão acaba, `MessageSync` apaga todas as tabelas com `clearAllTables()`, inclusive a fila de envio, e esquece o chatbot em memória. Isso vale para o logout, para uma renovação de token que o servidor recusou com 4xx e para uma troca direta de conta. O mesmo apagamento roda na abertura do app sem sessão. Uma falha de rede ou um 5xx na renovação não encerra a sessão. `backup_rules.xml` e `data_extraction_rules.xml` excluem `solaria.db` do backup e da transferência de aparelho, para o Android não restaurar o que o logout apagou.

## Navegação e detalhes

`AppDestination` define objetos tipados com Kotlin Serialization. `AppNavHost` é o único lugar que chama `NavController`; telas recebem callbacks e, quando necessário, um `AppDestination`, nunca strings de rota. Detalhes de projetos, conversas e perfis continuam demonstrativos/genéricos. IDs tipados só devem ser introduzidos quando o conteúdo do item selecionado realmente for exibido, para não fingir semântica que a UI ainda não tem.

## Tokens

Usar `SolariaColorScheme`/`MaterialTheme` e os tokens em `core/designsystem/theme`. Cores de marca, superfície e estados semânticos têm nomes; evitar hexadecimais nas telas. `SolariaSpace` e `SolariaRadius` cobrem valores compartilhados. Não é necessário transformar cada dimensão incidental de desenho em variável global: um token deve representar uma decisão de design repetida ou semântica, não apenas um número.

## Evolução por feature

1. Definir estado, eventos e contrato de dados antes de conectar a rota.
2. Manter `Route` pequena e `Screen` parametrizada; extrair componente para o design system apenas se houver reuso ou identidade visual compartilhada.
3. Trocar a implementação fake no binding Hilt quando houver API, sem colocar Retrofit em Composables.
4. Preservar comportamento e layout em refatorações; mudanças de produto ficam separadas.
5. Verificar `:app:assembleDebug`, `:app:testDebugUnitTest` e smoke tests instrumentados de navegação. Atualizar este documento e `ARCHITECTURE.md` quando os limites mudarem.

Referências: [UI layer](https://developer.android.com/topic/architecture/ui-layer), [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting) e [type-safe Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety).
