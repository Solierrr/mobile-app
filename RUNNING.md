# Rodando o Projeto Localmente

Este repositório é Kotlin + Jetpack Compose (Android), buildado com Gradle Kotlin DSL. Diferente dos serviços de backend da organização, não há `Dockerfile` nem deploy em cluster — o app roda direto em um emulador ou dispositivo físico via Android Studio. Antes de iniciar, verifique a seção de impedimentos abaixo.

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,android,androidstudio,jetpackcompose,github" height="48" alt="Rodando o Projeto — Jetpack Compose">
  </a>
</p>

## Possíveis Impedimentos

- **Android Studio instalado**, é a única IDE com suporte completo a emulador, Gradle Sync e preview de Compose — não é possível rodar o app corretamente em outra IDE.
- **JDK 21**, o CI (`ci.yml`) usa `temurin` 21 via `actions/setup-java`; use a mesma versão localmente para evitar builds divergentes do pipeline.
- **AGP 9.3.1 e Kotlin 2.2.10**, versões fixadas em `gradle/libs.versions.toml` — o `compileSdk`/`targetSdk` está em 37 e o `minSdk` em 29, então o SDK Platform 37 precisa estar instalado no SDK Manager do Android Studio.
- **Emulador ou dispositivo físico configurado**, um AVD (Android Virtual Device) precisa estar criado no Android Studio, ou um dispositivo físico com depuração USB habilitada.
- **Secrets locais**, o projeto ainda não define nenhuma chave de API no código-fonte, mas se a integração com as APIs da organização for adicionada, endpoints e credenciais devem ser criados manualmente em `local.properties` (não versionado) — sem isso, o app builda mas falha ao tentar se conectar em dependências externas.

## Instalação do Projeto

### Iniciando o repositório com o Github

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=github,androidstudio" height="48" alt="Frameworks">
  </a>
</p>

Clone o repositório e abra no Android Studio — deixe o Gradle Sync inicial terminar antes de rodar.

```Comandos para clonar o repositório
git clone https://github.com/Solierrr/mobile-app.git
cd ./mobile-app
studio .
```

### Instalando dependências e rodando o projeto localmente

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=gradle" height="48" alt="Frameworks">
  </a>
</p>

Use sempre o wrapper (`gradlew`/`gradlew.bat`) em vez de um Gradle instalado globalmente, para garantir a mesma versão usada no CI. Após o build, rode o app pelo próprio Android Studio (▶ Run) selecionando o emulador/dispositivo — o Gradle wrapper serve para validar o build e rodar os testes fora da IDE.

```Comandos para instalação de dependências
./gradlew clean build
```

### Testes e lint

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,gradle" height="48" alt="Testes">
  </a>
</p>

O pipeline de CI roda dois workflows separados em toda Pull Request: testes unitários (`ci.yml`) e lint (`quality.yml`) via `ktlint`. Rode os dois localmente antes de abrir a PR para evitar falhas no CI.

```Comandos de teste e lint
./gradlew testDebugUnitTest
./ktlint "app/src/**/*.kt"
```
