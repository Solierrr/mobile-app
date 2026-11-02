<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Variantes e distribuição

O aplicativo é instalado num aparelho ou emulador, sem deploy deste cliente no Render ou no cluster, e as variantes escolhem os serviços no momento do build.

## Destinos compilados

| Serviço | QA | PROD |
|---|---|---|
| Acesso | `https://api-auth-xw6o.onrender.com/` | `https://api-auth.solarianetwork.site/` |
| Cadastro | `https://api-core-cqfn.onrender.com/` | `https://api-core.solarianetwork.site/` |
| Mensagens | `https://api-messenger-5g2n.onrender.com/` | `https://api-messenger.solarianetwork.site/` |
| Endereços | `https://google-registry.onrender.com/` | Sem destino configurado |

O QA tem um identificador com o sufixo `.qa`, além de nome e esquema de callback próprios, o que permite instalar as variantes separadamente. Os hosts de produção e o contrato de endereços precisam ser confirmados antes da distribuição. <!-- PENDENTE: 2 -->

## Artefatos

O `make build FLAVOR=qa` gera `app/build/outputs/apk/qa/debug/app-qa-debug.apk`, e o PROD gera o equivalente em `prod/debug`, mas são builds debug e não comprovam um pacote assinado para distribuição.

O workflow de release apenas informa que a publicação ainda não foi configurada. Não monta APK, assina ou publica em loja. O checklist cita uma tag antiga, mas o Gradle declara `versionName` 1.0 e `versionCode` 1. Confirmar canal, assinatura e sequência de versão. <!-- PENDENTE: 3 -->

## Atualização

Uma mudança de versão do Room sem migração pode recriar o banco e perder mensagens pendentes, e a regra definida é que a fila sobreviva às atualizações do app, por isso cada mudança de versão precisa de migração.

O CI roda os testes unitários dos dois flavors com o JDK 21, o que não equivale a instrumentação, assinatura ou distribuição. O fluxo geral está em [release e deploy](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/RELEASE_AND_DEPLOY.md).

Fontes em [Gradle](https://github.com/Solierrr/mobile-app/blob/main/app/build.gradle.kts) e [workflow de release](https://github.com/Solierrr/mobile-app/blob/main/.github/workflows/release.yml).
