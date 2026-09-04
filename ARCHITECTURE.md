# Arquitetura do Repositório

O `mobile-app` segue a estrutura padrão de um projeto Android single-module gerado pelo Android Studio, com Gradle Kotlin DSL (`.kts`) e Jetpack Compose como toolkit de UI declarativa. Hoje existe um único módulo (`app`), sem separação em camadas de `data`/`domain`/`presentation` nem injeção de dependência configurada — a estrutura de arquitetura em camadas (MVVM, Compose Navigation, Hilt/Koin, etc) ainda {a confirmar}, já que o código atual corresponde ao template inicial "Empty Activity" do Android Studio, com apenas a `MainActivity` e o tema Compose (`ui/theme`). O pacote raiz é `com.project.solaria_mobile`, e o build é validado por dois workflows no GitHub Actions: `ci.yml` (testes unitários via `gradlew testDebugUnitTest`) e `quality.yml` (lint via `ktlint`), além de análise estática configurada no SonarQube (`sonar-project.properties`).

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,android,jetpackcompose,gradle" height="48" alt="Arquitetura">
  </a>
</p>

- **Módulo único (`app`)**, não há multi-módulo nem separação `core`/`feature` — todo o código-fonte vive em `app/src`.
- **Gradle Kotlin DSL + Version Catalog**, dependências e versões centralizadas em `gradle/libs.versions.toml`, referenciadas nos `build.gradle.kts` via `libs.*` em vez de strings soltas.
- **Jetpack Compose para UI**, telas declarativas com `@Composable`, sem uso de XML layouts ou View system tradicional; tema centralizado em `ui/theme` (`Color.kt`, `Theme.kt`, `Type.kt`).
- **Sem camada de rede/persistência ainda**, não há cliente HTTP, banco local ou repositórios configurados no projeto — a integração com as APIs da organização {a confirmar}.
- **Testes separados por tipo**, `app/src/test` para testes unitários (JVM, JUnit4) e `app/src/androidTest` para testes instrumentados (Espresso + Compose UI Test), rodando em dispositivo/emulador.
- **CI/CD via GitHub Actions**, pipeline de teste e lint roda em toda Pull Request e push em `main`; o workflow de release (`release.yml`) existe mas ainda não está implementado.

```Tree do Repositório
├── .github/
│   ├── workflows/
│   │   ├── ci.yml
│   │   ├── quality.yml
│   │   ├── release.yml
│   │   └── sonarqube.yml
│   └── pull_request_template.md
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/project/solaria_mobile/
│       │   │   ├── MainActivity.kt
│       │   │   └── ui/theme/
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   ├── keepRules/
│       │   │   └── rules.keep
│       │   └── res/
│       │       ├── drawable/
│       │       ├── mipmap-*/
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       ├── test/
│       │   └── java/com/project/solaria_mobile/ExampleUnitTest.kt
│       └── androidTest/
│           └── java/com/project/solaria_mobile/ExampleInstrumentedTest.kt
├── gradle/
│   ├── libs.versions.toml
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── sonar-project.properties
├── gradlew
├── gradlew.bat
├── README.md
├── ARCHITECTURE.md
├── RUNNING.md
└── LICENSE
```
