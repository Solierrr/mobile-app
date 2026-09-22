# Arquitetura de Frontend (Jetpack Compose)

Este documento descreve a arquitetura de apresentação/UI adotada neste app, escrita
para quem já é confortável com React, Vue, Java e Spring, mas está vendo Jetpack
Compose pela primeira vez. A ideia não é "aprender Android do zero", e sim mapear
os conceitos de Compose para o vocabulário que você já usa no frontend web.

## TL;DR das analogias

| Compose / Android                          | Equivalente que você já conhece                                   |
|---------------------------------------------|---------------------------------------------------------------------|
| `@Composable fun Foo(...)`                  | Componente funcional React / componente de função Vue (setup)      |
| Parâmetros de uma função `@Composable`      | `props`                                                              |
| State hoisting (estado subindo para o pai)  | "Lifting state up" no React / `v-model` + prop+emit no Vue          |
| `remember { mutableStateOf(x) }`            | `useState(x)` (React) / `ref(x)` (Vue) - estado local do componente |
| `ViewModel`                                 | Store de tela (tipo um Pinia store ou um slice de Redux, escopado à tela) |
| `StateFlow` exposto pelo ViewModel          | O valor observável de um store (getter reativo do Pinia / selector do Redux) |
| `collectAsStateWithLifecycle()`             | `useSelector()` / `storeToRefs()` - assina o store e vira estado de UI |
| `LaunchedEffect(key) { ... }`               | `useEffect(() => {...}, [key])`                                     |
| Navigation Compose (`NavHost`, `composable`)| React Router / Vue Router (rotas -> tela)                           |
| Hilt (`@Inject`, `@HiltViewModel`, módulos) | Spring DI (`@Autowired`, `@Component`, `@Bean`, `@Service`)          |
| Recomposição                                | Re-render (React) / reatividade (Vue) - Compose decide o que re-executar de forma granular, parecido com o Vue 3 (Proxy/reatividade fina) mais do que com o "re-render da árvore inteira" ingênuo do React |
| `Modifier`                                  | Algo entre `className`/`style` (aparência/layout) e `props` de comportamento (clique, padding, etc), encadeado como um builder |

Compose é **declarativo**: você descreve "como a UI deveria ser para este estado",
e o framework decide o que precisa mudar na tela, como já fez em React/Vue.
Não existe manipulação manual de View (o `findViewById` do Android "antigo"
morreu no Compose, assim como manipular o DOM manualmente morreu com React/Vue).

## Estado do projeto (checado antes de escrever este documento)

O repositório está em estágio inicial - `ARCHITECTURE.md` já confirma isso.
Antes desta branch, o único código-fonte era o template "Empty Activity" do Android
Studio (`MainActivity` + tema em `ui/theme`), e já existiam pastas vazias
(`core/common`, `core/designsystem/components`, `core/designsystem/theme`,
`core/network`, `data`, `di`, `domain`, `presentation`) marcadas só com `.gitkeep`,
indicando que a divisão de pacotes abaixo já era a intenção do time, mas nada
tinha sido implementado. Este trabalho preenche o mínimo necessário para a camada
de apresentação (`core/designsystem/theme` e `presentation/`) com um exemplo real,
sem inventar uma estrutura nova.

## Camadas da apresentação

```
presentation/
├── home/
│   ├── HomeViewModel.kt   # estado + eventos da tela "Home"
│   └── HomeScreen.kt      # HomeRoute (com estado) + HomeScreen (sem estado)
└── navigation/
    └── AppNavHost.kt      # grafo de rotas (equivalente a <Routes> do React Router)

core/designsystem/
├── theme/                 # Theme.kt, Color.kt, Type.kt - "design tokens" + ThemeProvider
└── components/            # (ainda vazio) componentes reutilizáveis do design system
```

### 1. UI Composables (`presentation/<feature>/`)

Cada tela vira um pacote sob `presentation/`, com (pelo menos) dois tipos de
Composable, seguindo o padrão "container / presentational" que você já usa em
React:

- **Route Composable** (`HomeRoute`): o "container". Obtém o `ViewModel` (via
  `viewModel()`, o equivalente a chamar um hook de store), assina o estado
  (`collectAsStateWithLifecycle()`), dispara efeitos (`LaunchedEffect`) e repassa
  tudo como parâmetros simples para o Composable de baixo. É o único lugar que
  "sabe" que existe um ViewModel.
- **Screen Composable** (`HomeScreen`): "presentational"/"dumb component".
  Recebe só dados primitivos e lambdas (`uiState: HomeUiState`,
  `onNameChanged: (String) -> Unit`), não conhece ViewModel nem navegação.
  Isso é o que permite usar `@Preview` (o "Storybook" do Compose) sem precisar
  de contexto de app rodando, e testar a UI isoladamente.

Essa separação é a mesma razão pela qual, em React, você evita colocar `useSelector`
espalhado em componentes de folha: centraliza a conexão com o "store" no
componente de topo da feature e deixa o resto burro/reutilizável.

### 2. State & ViewModel (padrão MVVM + fluxo unidirecional)

O `ViewModel` (`androidx.lifecycle.ViewModel`) é uma classe que:

- **Sobrevive a recomposições e a mudanças de configuração** (ex: rotação de
  tela), porque não vive "dentro" do Composable - o Android Framework mantém
  uma instância por destino de navegação/Activity, parecido com como um store
  Pinia/Redux existe fora do ciclo de vida de um componente Vue/React específico.
- **Expõe estado somente-leitura** via `StateFlow` (`val uiState: StateFlow<HomeUiState>`),
  igual a um getter reativo/computed de um store: a UI só lê, nunca escreve
  diretamente no `StateFlow`.
- **Expõe eventos como métodos públicos** (`onNameChanged(newName)`), que a UI
  chama em resposta a interação do usuário - o equivalente a `dispatch(action)`
  no Redux ou a chamar uma `action`/método do store no Pinia.

Isso implementa **fluxo unidirecional de dados** (unidirectional data flow),
o mesmo princípio do Redux/Flux e do Pinia:

```
Evento de UI -> método do ViewModel -> novo estado (StateFlow) -> UI recompõe
```

Nunca o contrário: a UI não muta o estado do ViewModel diretamente, do mesmo jeito
que um componente React não deveria mutar o state do Redux store na mão.

### 3. State hoisting

Dentro de uma mesma feature, o mesmo princípio se aplica em miniatura entre
Composables: o estado "sobe" para quem precisa compartilhá-lo/controlá-lo, e
desce como parâmetro + callback para quem só precisa exibi-lo/reagir a ele.
`HomeScreen(uiState, onNameChanged)` é literalmente "lifting state up": o pai
(`HomeRoute`, apoiado no ViewModel) é a fonte da verdade; o filho é controlado
(*controlled component*, no vocabulário React) - o mesmo padrão de
`<input value={x} onChange={e => setX(e.target.value)} />`.

Para estado que é **puramente de UI e local a um único Composable** (ex: se um
`DropdownMenu` está aberto), não é preciso subir isso até o ViewModel - use
`remember { mutableStateOf(...) }`, equivalente a um `useState`/`ref` que não
precisa ser compartilhado.

### 4. Efeitos (`LaunchedEffect`)

`LaunchedEffect(chave) { ... }` roda uma coroutine quando o Composable entra em
composição (e recomeça se `chave` mudar), sendo cancelada automaticamente quando
o Composable sai de tela - o paralelo direto de `useEffect(() => {...}, [chave])`.
No exemplo (`HomeRoute`), é usado para disparar `viewModel.loadInitialData()` uma
única vez (`LaunchedEffect(Unit)`), do mesmo jeito que um `useEffect(() => {...}, [])`
dispararia um fetch inicial.

### 5. Navigation (`presentation/navigation/AppNavHost.kt`)

Navigation Compose é o React Router/Vue Router deste app: um `NavHost` mapeia
rotas (strings, como `"home"`) para o Composable que deve ser renderizado,
igual a um `<Route path="/home" element={<Home />} />`. Novas telas entram como
novas entradas `composable("rota") { TelaRoute() }` dentro do mesmo grafo, e
navegação entre telas usa o `NavHostController` (`navController.navigate("rota")`),
parecido com `useNavigate()`/`router.push()`.

### 6. Design System / Theming (`core/designsystem/`)

- `theme/Theme.kt` define `SolariamobileTheme`, que envolve toda a árvore de UI
  (chamado uma vez em `MainActivity`) e disponibiliza cores/tipografia via
  `MaterialTheme.colorScheme` / `MaterialTheme.typography` para qualquer
  Composable descendente - o mesmo papel de um `<ThemeProvider theme={...}>`
  do styled-components/MUI, ou do plugin de tema do Vuetify. É "Context API"
  por baixo dos panos (Compose tem seu próprio mecanismo de contexto local,
  `CompositionLocal`, que é o que `MaterialTheme` usa).
- `theme/Color.kt` / `Type.kt` são os *design tokens* (paleta, escalas de
  tipografia) - o equivalente a um `tokens.ts`/arquivo de variáveis de tema.
- `designsystem/components/` (ainda vazio, mantido como `.gitkeep`) é onde
  devem morar componentes visuais reutilizáveis entre features (botões, cards,
  inputs com o estilo da marca) - o "component library" interno do app, o
  paralelo de uma pasta `components/ui` compartilhada entre páginas numa SPA.

## Estrutura de pacotes sugerida (e por quê)

```
com.project.solaria_mobile/
├── MainActivity.kt              # composition root: monta tema + NavHost
├── core/
│   ├── common/                  # utilitários sem dependência de Android específica
│   ├── designsystem/
│   │   ├── theme/               # tokens + ThemeProvider
│   │   └── components/          # componentes de UI reutilizáveis
│   └── network/                 # cliente HTTP/config de rede (futuro)
├── data/                        # repositórios, fontes de dados (API, local) - implementa contratos do domain
├── domain/                      # casos de uso / regras de negócio puras, sem Android/Compose
├── di/                          # módulos Hilt (bindings de repositório, network, etc)
└── presentation/
    ├── home/                    # uma pasta por feature/tela
    │   ├── HomeViewModel.kt
    │   └── HomeScreen.kt
    └── navigation/
        └── AppNavHost.kt
```

Isso é, essencialmente, uma variação do **MVVM** que o próprio Google recomenda
para Compose, com uma separação adicional `data`/`domain` inspirada em Clean
Architecture - o equivalente, no mundo Spring, a separar `controller` (~`presentation`),
`service`/`usecase` (~`domain`) e `repository` (~`data`). Cada feature em
`presentation/` só depende de `domain` (casos de uso), nunca diretamente de
`data` - do mesmo jeito que um `@RestController` do Spring não deveria falar
direto com o driver JDBC, e sim com um `@Service`.

## Dependency Injection (Hilt) - próximos passos

O projeto ainda **não** tem Hilt configurado (não havia nenhuma dependência de
DI no `libs.versions.toml` antes desta branch, e nenhum `@HiltAndroidApp`).
Quando `data`/`domain`/`di` começarem a ser implementados de verdade, o padrão
esperado é:

- `@HiltAndroidApp` na classe `Application` (equivalente ao `@SpringBootApplication`
  como ponto de entrada que liga o container de DI).
- `@HiltViewModel` nos ViewModels que passam a ter dependências (ex:
  `class HomeViewModel @Inject constructor(private val getUserUseCase: GetUserUseCase)`) -
  o `@Inject constructor` é literalmente o mesmo mecanismo de injeção por
  construtor que você já usa em `@Service`/`@Component` do Spring.
  Composables então obtêm o ViewModel via `hiltViewModel()` em vez de `viewModel()`.
- Módulos em `di/` com `@Module` + `@InstallIn(...)` + `@Provides`/`@Binds` -
  o equivalente às classes `@Configuration` com métodos `@Bean` do Spring.

Como isso ainda depende de decisões sobre `data`/`network` (marcadas como
"a confirmar" no `ARCHITECTURE.md`), Hilt não foi adicionado nesta branch para
não introduzir infraestrutura sem uso real - o `HomeViewModel` de exemplo
ainda é instanciado sem dependências (`viewModel()` puro), e deve migrar para
`@HiltViewModel`/`hiltViewModel()` assim que a primeira feature precisar de
um repositório de verdade.

## O que foi adicionado nesta branch

- `core/designsystem/theme/{Color,Type,Theme}.kt`: tema movido do antigo
  `ui/theme` (sourceset `java/`) para o pacote/sourceset já esperado pela
  estrutura descrita no `ARCHITECTURE.md` (sourceset `kotlin/`).
- `presentation/home/HomeViewModel.kt` e `HomeScreen.kt`: exemplo mínimo e
  comentado de ViewModel + state hoisting + `LaunchedEffect`, para servir de
  modelo às próximas telas.
- `presentation/navigation/AppNavHost.kt`: grafo de navegação mínimo (uma
  única rota, `home`), para não deixar `MainActivity` acoplada diretamente a
  uma tela.
- `MainActivity.kt`: agora só monta o tema e delega para `AppNavHost`.
- Dependências adicionadas ao `gradle/libs.versions.toml` / `app/build.gradle.kts`:
  `androidx.navigation:navigation-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`
  e `androidx.lifecycle:lifecycle-runtime-compose` (necessárias para
  `viewModel()`, `collectAsStateWithLifecycle()` e o `NavHost`).

`core/common`, `core/network`, `data`, `domain` e `di` seguem vazios
(`.gitkeep`) propositalmente - ainda não há integração com APIs nem regra de
negócio real para justificar código neles (ver "a confirmar" no
`ARCHITECTURE.md`/`README.md`).
