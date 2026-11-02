<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Leitura e preferências

A leitura é guardada sem rede. Arquivar e silenciar exigem conexão.

```mermaid
flowchart TD
  A[Ler conversa] --> B[Avançar leitura local]
  B --> C{Há rede?}
  C -->|Não| D[Manter leitura pendente]
  D --> C
  C -->|Sim| E[PATCH com JWT]
  E -->|Sucesso| F[Aplicar estado do servidor]
  E -->|Falha temporária| D
  E -->|Validação ou acesso perdido| G[Descartar leitura recusada]
  H[Arquivar ou silenciar] --> I{Há rede?}
  I -->|Não| J[Exibir falha de conexão]
  I -->|Sim| E
```

## Falhas e limites

O worker de leitura repete em 30 segundos ou por gatilho. O cursor local pode zerar a contagem exibida antes da confirmação. Estado pessoal não usa tabela de fila adicional.

Fonte em [implementação](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/communication/data/ConversationStateSync.kt).
