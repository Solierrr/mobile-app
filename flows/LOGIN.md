<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Login pelo navegador

Ao abrir sem sessão, o aplicativo inicia autorização no navegador e recebe código e state no callback.

```mermaid
sequenceDiagram
  participant P as Pessoa
  participant M as Aplicativo
  participant A as Serviço de acesso
  participant B as Navegador
  P->>M: Abrir aplicativo
  M->>M: Gerar verifier e state
  M->>A: HTTPS iniciar transação PKCE S256
  A-->>M: URL de autorização
  M->>M: Guardar pendência
  M->>B: Abrir autorização
  B->>A: Autenticar e autorizar
  A-->>M: Deep link com code e state
  M->>M: Consumir pendência e comparar state
  alt Callback válido
    M->>A: HTTPS trocar código com verifier
    A-->>M: Tokens
    M->>M: Cifrar e guardar sessão
  else Callback inválido
    M-->>P: Falha de sessão
  end
```

## Falhas e limites

Um code ou state ausente, uma pendência inexistente ou um state diferente impedem a troca, e a pendência só pode ser consumida uma vez, cabendo ao servidor decidir a validade do código e a elegibilidade. O contexto está em [login e token](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/LOGIN_AND_TOKEN.md).

Fonte em [implementação](https://github.com/Solierrr/mobile-app/blob/main/app/src/main/kotlin/com/project/solaria_mobile/feature/auth/data/RemoteAuthRepository.kt).
