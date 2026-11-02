<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Banco local

O SQLite local usa Room e pertence ao aplicativo. Guarda cache de conversas humanas e fila de envio, sem ser fonte de verdade do cadastro ou do serviço de mensagens. A versão é 3, com cinco entidades e `exportSchema=false`.

O módulo define o nome físico do banco e usa `fallbackToDestructiveMigration(true)`, sem declarar uma migração que preserve os dados. A fila de mensagens não enviadas precisa sobreviver às atualizações do app, e as migrações que garantem isso estão pendentes.

## Estrutura

O diagrama lista os campos do schema, e as referências entre identificadores são apenas lógicas, já que as entidades não declaram chaves estrangeiras.

```mermaid
erDiagram
  conversations {
    String id PK
    String syncEpoch
    Long syncVersion
    String conversationType
    String participantIds
    String title
    Boolean archived
    Long lastInteractionAt
    String lastMessageId
    Int lastMessageSequence
    String lastMessageContent
    Boolean lastMessageDeleted
    Boolean accessLost
    Int lastReadSequence
    Int localReadSequence
    Long unreadCount
    Boolean muted
  }
  messages {
    String id PK
    String conversationId
    Int sequence
    String senderId
    String clientMessageId
    String content
    Boolean edited
    Boolean deleted
    Long timestamp
    Long changeVersion
  }
  pending_messages {
    Long localId PK
    String clientMessageId
    String senderId
    String conversationId
    String messageType
    String content
    Long createdAt
    Int attempts
    Boolean failed
  }
  users {
    String authId PK
    String username
    String avatar
  }
  sync_state {
    Int id PK
    String syncEpoch
    Long cursor
  }
```

## Tabelas e índices

| Tabela | Chave e índices explícitos | Conteúdo e acesso |
|---|---|---|
| `conversations` | PK `id` | Tipo, participantes, prévia, época, versão, acesso e estado pessoal. Sincronização e estado escrevem. Lista e chat leem |
| `messages` | PK `id`, índice único em `conversationId, sequence` | Mensagens humanas, versão, edição e tombstone. MessageStore aplica merge e a tela observa |
| `pending_messages` | PK autogerada `localId`, índice único `clientMessageId`, índice `senderId, conversationId` | Texto, destino, remetente, tentativas e falha. Outbox grava antes do POST e remove após confirmar ou descartar |
| `users` | PK `authId` | Nome e avatar do chat. UserDirectory preenche pelo cadastro central |
| `sync_state` | PK `id`, uma linha convencionada como zero | Época e cursor. Somente catch-up avança o cursor |

Há três índices explícitos além das chaves primárias, e o `conversationId` referencia logicamente `conversations.id`, enquanto o `senderId` é uma identidade que pode não ter linha em `users`. O `clientMessageId` associa a pendência à confirmação, e nenhuma cascata relacional é declarada.

## Leitura e escrita

As mensagens são ordenadas por `sequence`, e o merge transacional aceita a versão mais nova e remove o conteúdo na exclusão, enquanto as conversas usam época e versão para descartar eventos antigos. O cursor é gravado depois que a página é aplicada, o que permite reaplicá-la após uma interrupção.

`localReadSequence` é a leitura no aparelho. A diferença para `lastReadSequence` representa pendência sem tabela de fila. A contagem exibida fica zero quando a pessoa já viu a última mensagem localmente.

Chatbots podem ter linha na lista, mas suas mensagens não ficam no cache persistente. DataStore guarda a sessão fora destas tabelas, com tokens cifrados pelo Keystore.

## Ciclo dos dados

Logout ou troca de conta limpa todas as tabelas. Perda de acesso mantém conteúdo local em leitura. Mudança de época limpa mensagens e cursor e refaz sincronização, mantendo pendências. Migração destrutiva pode perder também a fila, portanto cache recuperável e mensagem não enviada têm riscos diferentes.

Não foi encontrada limpeza por TTL nem limite de histórico nesse caminho, e a sincronização tenta completar o histórico humano até o início, sem teto de armazenamento definido.

Fontes em [entidades e DAOs](https://github.com/Solierrr/mobile-app/tree/main/app/src/main/kotlin/com/project/solaria_mobile/core/database), [registro das entidades](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/core/database/AppDatabase.kt), [abertura](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/di/DatabaseModule.kt) e [sincronização](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/communication/data/MessageSync.kt).
