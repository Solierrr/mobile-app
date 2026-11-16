# Arquitetura do mobile-app

Aplicativo Android em Kotlin e Jetpack Compose, com um módulo Gradle (`:app`), organização por feature e MVVM na camada de apresentação. Repositórios remotos consomem os contratos dos serviços; o modo mock é uma opção exclusiva do QA. A comunicação usa o api-messenger, WebSocket e cache Room, descritos em [FRONTEND_ARCHITECTURE.md](FRONTEND_ARCHITECTURE.md#chat-com-cache-local). Serviços técnicos e notificações têm repositórios próprios.

```text
app/src/main/kotlin/com/project/solaria_mobile/
├── MainActivity.kt
├── App.kt
├── core/database/                      # Room: mensagens, conversas, nomes e fila de envio
├── core/designsystem/{theme,components}/
├── presentation/navigation/{AppDestination,AppNavHost}.kt
└── feature/
    ├── home/{data,HomeViewModel,HomeScreen,SplashScreen,...}
    ├── project/{data,ProjectListViewModel,ProjectListScreen,ProjectDetailScreen}
    ├── communication/{data,ConversationsViewModel,ChatViewModel,CallsViewModel,...}
    ├── discovery/{data,ScheduleViewModel,MapViewModel,...}
    └── profile/{data,ProfileViewModel,ConnectionsViewModel,SettingsViewModel,...}
```

Os pacotes `data` de cada feature contêm contratos Retrofit, implementações remotas e bindings Hilt. Fakes ficam nos testes; o QA simula respostas por `MockApiInterceptor`. Os ViewModels expõem `StateFlow` imutável e recebem eventos da UI. Cada rota coleta estado com atenção ao ciclo de vida e chama uma tela parametrizada; componentes locais são privados e os compartilhados vivem no design system. Estado transitório estritamente visual, como abertura de uma sheet, permanece no Composable.

`AppDestination` usa objetos serializáveis e `AppNavHost` centraliza a navegação tipada. Telas não conhecem `NavController`. Os detalhes recebem o identificador da rota e consultam o repositório correspondente.

O tema usa Plus Jakarta Sans e paleta/tokens Solaria. Os ícones compartilhados passam por `AppIcons`. As dependências ficam no Version Catalog em `gradle/libs.versions.toml`.

Testes de ViewModel ficam em `app/src/test`; testes de interface ficam em `app/src/androidTest`. QA: `./gradlew :app:testQaDebugUnitTest :app:assembleQaDebug`. Produção: `./gradlew :app:testProdDebugUnitTest :app:assembleProdDebug`. Instrumentação requer dispositivo/emulador e o flavor correspondente. A configuração de ambiente usada para compilar está em [QA.md](QA.md).

Para limites de responsabilidade e fluxo de dados, consulte [FRONTEND_ARCHITECTURE.md](FRONTEND_ARCHITECTURE.md). Para as telas e seus fluxos, consulte [MOBILE_UI.md](MOBILE_UI.md).
