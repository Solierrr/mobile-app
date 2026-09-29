# Arquitetura do mobile-app

Aplicativo Android em Kotlin e Jetpack Compose, com um módulo Gradle (`:app`), organização por feature e MVVM na camada de apresentação. O estado atual é uma UI demonstrativa: não há autenticação, API conectada nem persistência local nesta entrega.

```text
app/src/main/kotlin/com/project/solaria_mobile/
├── MainActivity.kt
├── SolariaApplication.kt
├── core/designsystem/{theme,components}/
├── presentation/navigation/{AppDestination,AppNavHost}.kt
└── feature/
    ├── home/{data,HomeViewModel,HomeScreen,SplashScreen,...}
    ├── project/{data,ProjectListViewModel,ProjectListScreen,ProjectDetailScreen}
    ├── communication/{data,ConversationsViewModel,ChatViewModel,CallsViewModel,...}
    ├── discovery/{data,ScheduleViewModel,MapViewModel,...}
    └── profile/{data,ProfileViewModel,ConnectionsViewModel,SettingsViewModel,...}
```

Os pacotes `data` de cada feature contêm contrato, implementação fake em memória e binding Hilt. Os ViewModels expõem `StateFlow` imutável e recebem eventos da UI. Cada rota coleta estado com atenção ao ciclo de vida e chama uma tela parametrizada; componentes locais são privados e os compartilhados vivem no design system. Estado transitório estritamente visual, como abertura de uma sheet, permanece no Composable.

`AppDestination` usa objetos serializáveis e `AppNavHost` centraliza a navegação tipada. Telas não conhecem `NavController`. Os detalhes continuam genéricos, como eram antes da refatoração; selecionar um item ainda não altera o conteúdo do detalhe.

O tema usa Plus Jakarta Sans e paleta/tokens Solaria. Os ícones usam Lucide Compose. As dependências ficam no Version Catalog em `gradle/libs.versions.toml`.

Testes de ViewModel ficam em `app/src/test`; o smoke test de navegação fica em `app/src/androidTest`. Comandos previstos: `./gradlew :app:testDebugUnitTest`, `./gradlew :app:assembleDebug` e `./gradlew :app:connectedDebugAndroidTest` com dispositivo/emulador.

Para limites de responsabilidade, fluxo de dados e analogias com React, consulte [docs/FRONTEND_ARCHITECTURE.md](docs/FRONTEND_ARCHITECTURE.md). Para as telas e seus fluxos, consulte [docs/MOBILE_UI.md](docs/MOBILE_UI.md).
