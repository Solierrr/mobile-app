<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Criação de projeto

A tela valida o rascunho e envia ao cadastro central, abrindo o id retornado.

```mermaid
flowchart TD
  A[Preencher projeto] --> B[Validar nome e campos]
  B -->|Inválido| C[Exibir campos com erro]
  B -->|Válido| D[Marcar envio em andamento]
  D --> E[HTTPS POST com JWT]
  E -->|Sucesso| F[Abrir detalhe retornado]
  E -->|Falha| G[Exibir categoria de falha]
  F --> H[Consultar seções do projeto]
```

## Falhas e limites

O ViewModel bloqueia uma nova submissão durante o envio, e a validação local não substitui a autorização do servidor. A localização é omitida quando todos os campos estão vazios, e uma falha não cria um projeto local substituto.

Fonte em [implementação](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/project/NewProjectViewModel.kt).
