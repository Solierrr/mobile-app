<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Sincronização do histórico

O processo combina eventos ao vivo, catch-up por cursor e reparo de histórico humano.

```mermaid
sequenceDiagram
  participant A as Aplicativo
  participant S as Serviço de mensagens
  participant D as Room local
  A->>S: STOMP com JWT e tópico privado
  A->>S: HTTPS listar ativas e arquivadas
  S-->>A: Conversas e versões
  A->>D: Atualizar conversas
  A->>S: HTTPS catch-up por cursor
  alt Época mudou
    A->>D: Limpar mensagens e cursor
    A->>S: Bootstrap da época
  else Época preservada
    S-->>A: Página de mudanças
    A->>D: Aplicar e depois avançar cursor
  end
  A->>S: HTTPS completar lacunas e histórico
  S-->>A: Mensagens paginadas
  A->>D: Merge por versão
  S-->>A: Evento STOMP
  A->>D: Aplicar sem avançar cursor
```

## Falhas e limites

Uma falha com rede agenda uma nova sincronização em 30 segundos, e sem rede o app aguarda um gatilho, enquanto os chatbots e as conversas sem acesso não recebem o reparo humano. Uma página que não muda o resumo encerra o reparo para evitar laço, e o contexto está em [comunidade de projeto](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/PROJECT_COMMUNITY.md).

Fonte em [implementação](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/communication/data/MessageSync.kt).
