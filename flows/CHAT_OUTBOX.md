<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Envio de mensagens

A mensagem é persistida antes do envio e sai da fila ao confirmar ou descartar.

```mermaid
flowchart TD
  A[Pessoa envia texto] --> B[Guardar pendência no Room]
  B --> C{Há rede e sessão?}
  C -->|Não| D[Aguardar gatilho]
  C -->|Sim| E{Já tentou antes?}
  E -->|Sim| F[Consultar últimas 100 mensagens]
  F -->|Encontrada| J[Aplicar confirmação e remover pendência]
  F -->|Ausente| G[Incrementar tentativa e POST com JWT]
  E -->|Não| G
  G -->|Confirmada| J
  G -->|Falha temporária| H[Esperar e tentar novamente]
  H --> C
  G -->|Validação ou acesso ou destino| I[Marcar falha]
  G -->|Sessão inválida| D
```

## Falhas e limites

Rede, 5xx, 429 ou conflito permite repetição com espera de 15, 30, 60, 120 e até 240 segundos. Validação, 403 ou 404 marca falha sem travar outras mensagens. A pessoa pode tentar ou descartar. Consultar só histórico recente não garante envio único. <!-- PENDENTE: 5 -->

Fonte em [implementação](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/communication/data/MessageOutbox.kt).
