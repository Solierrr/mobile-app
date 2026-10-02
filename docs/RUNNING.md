# Rodando o Projeto Localmente

Este repositório é Kotlin + Jetpack Compose (Android), buildado com Gradle Kotlin DSL. Diferente dos serviços de backend da organização, não há `Dockerfile` nem deploy em cluster — o app roda direto em um emulador ou dispositivo físico e pode ser operado pelo terminal com o `Makefile`, sem depender do Android Studio. Antes de iniciar, verifique a seção de impedimentos abaixo.

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,android,androidstudio,jetpackcompose,github" height="48" alt="Rodando o Projeto — Jetpack Compose">
  </a>
</p>

## Possíveis Impedimentos

- **Android SDK Command-line Tools**, com `adb`, `emulator`, Platform-Tools e o SDK Platform 37 disponíveis no `PATH`. O Android Studio é opcional.
- **JDK 21**, o CI (`ci.yml`) usa `temurin` 21 via `actions/setup-java`; use a mesma versão localmente para evitar builds divergentes do pipeline.
- **AGP 9.3.1 e Kotlin 2.2.10**, versões fixadas em `gradle/libs.versions.toml` — o `compileSdk`/`targetSdk` está em 37 e o `minSdk` em 29, então o SDK Platform 37 precisa estar instalado pelo `sdkmanager`.
- **Emulador ou dispositivo físico configurado**, um AVD (Android Virtual Device) precisa estar criado pelo `avdmanager`, ou um dispositivo físico com depuração USB habilitada.
- **Secrets locais**, o projeto ainda não define nenhuma chave de API no código-fonte, mas se a integração com as APIs da organização for adicionada, endpoints e credenciais devem ser criados manualmente em `local.properties` (não versionado) — sem isso, o app builda mas falha ao tentar se conectar em dependências externas.

## Instalação do Projeto

### Iniciando o repositório com o Github

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=github,androidstudio" height="48" alt="Frameworks">
  </a>
</p>

Clone o repositório e entre no diretório do projeto. O Gradle wrapper baixa as dependências necessárias no primeiro comando.

```Comandos para clonar o repositório
git clone https://github.com/Solierrr/mobile-app.git
cd ./mobile-app
```

### Instalando dependências e rodando o projeto localmente

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=gradle" height="48" alt="Frameworks">
  </a>
</p>

Use o `Makefile` para verificar o ambiente, listar ou iniciar um emulador e executar o app. Se houver mais de um dispositivo conectado, informe o serial com `DEVICE` (por exemplo, `make run DEVICE=emulator-5554`).

```bash
make doctor
make avds
make emulator AVD=nome_do_avd

# Em outro terminal, depois que o emulador iniciar:
make run
```

Use `make help` para ver todos os comandos. A criação inicial de um AVD pode ser feita com o `avdmanager`, também incluído nas Android SDK Command-line Tools.

### Testes e lint

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,gradle" height="48" alt="Testes">
  </a>
</p>

O pipeline de CI roda dois workflows separados em toda Pull Request: testes unitários (`ci.yml`) e lint (`quality.yml`) via `ktlint`. Rode os dois localmente antes de abrir a PR para evitar falhas no CI.

```bash
make check
```
