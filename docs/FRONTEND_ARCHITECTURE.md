# Arquitetura do frontend Compose

O app usa um módulo Gradle (`:app`) e organiza o código por feature. Esta refatoração mantém as telas e os fluxos existentes; dados ainda são demonstrativos, sem conexão com backend ou persistência. O Figma não é alterado nesta etapa.

## Estrutura

```text
com/project/solaria_mobile/
├── MainActivity.kt                    # tema e NavHost
├── SolariaApplication.kt              # Hilt
├── core/designsystem/
│   ├── theme/                         # cores, tipografia, espaçamento e raios
│   └── components/                    # componentes compartilhados, por responsabilidade
├── presentation/navigation/
│   ├── AppDestination.kt              # destinos @Serializable
│   └── AppNavHost.kt                  # único mapeamento de destinos para rotas
└── feature/
    ├── home/
    ├── project/
    ├── communication/
    ├── discovery/
    └── profile/
        ├── data/                     # contrato + FakeRepository + binding Hilt
        ├── *ViewModel.kt              # UiState, eventos e transformações
        └── *Screen.kt                 # Route + Screen + componentes locais
```

Uma feature pode ter várias rotas. O nome do arquivo acompanha a tela, e pequenos componentes privados permanecem próximos da tela que os usa. Componentes públicos reutilizados entre features ficam em `core/designsystem/components`. Não se cria um arquivo para cada `Text`, `Row` ou detalhe de layout.

## Responsabilidades e fluxo

| Peça | Responsabilidade | Analogia aproximada no React |
| --- | --- | --- |
| `Route` | Obtém o ViewModel via Hilt, coleta `StateFlow` com `collectAsStateWithLifecycle()` e conecta eventos/navegação. | Container conectado a uma store. |
| `Screen` | Recebe estado imutável e callbacks; desenha a UI sem buscar nem mutar dados. | Componente controlado por props. |
| `ViewModel` | Expõe `UiState` somente-leitura, filtra listas, trata eventos e chama repositórios. | Store escopada à rota. |
| `Repository` | Contrato para dados e ações. A implementação fake usa `MutableStateFlow` em memória e é `@Singleton`. | Serviço/store de dados compartilhado. |
| Design system | Fonte de tokens e componentes reutilizáveis. | Tokens e biblioteca de UI. |

O fluxo é `interação → callback da Screen → ViewModel → Repository/StateFlow → UiState → recomposição`. Não passar o ViewModel para componentes de folha. Estado puramente visual, como bottom sheet aberta, pode ficar em `remember`; mensagens, busca, calendário e ações compartilhadas ficam fora da UI.

Cada repositório fake é uma fonte única de verdade por feature enquanto o processo do app vive. Enviar mensagem e salvar localização atualiza o mesmo estado observado por outras rotas; não há DataStore nem chamada HTTP nesta versão. `HomeViewModel` usa também `ProjectRepository` para os projetos recentes. Previews usam fixtures próprias no source set `debug`, não Hilt.

## Navegação e detalhes

`AppDestination` define objetos tipados com Kotlin Serialization. `AppNavHost` é o único lugar que chama `NavController`; telas recebem callbacks e, quando necessário, um `AppDestination`, nunca strings de rota. Detalhes de projetos, conversas e perfis continuam demonstrativos/genéricos. IDs tipados só devem ser introduzidos quando o conteúdo do item selecionado realmente for exibido, para não fingir semântica que a UI ainda não tem.

## Tokens

Usar `SolariaColorScheme`/`MaterialTheme` e os tokens em `core/designsystem/theme`. Cores de marca, superfície e estados semânticos têm nomes; evitar hexadecimais nas telas. `SolariaSpace` e `SolariaRadius` cobrem valores compartilhados. Não é necessário transformar cada dimensão incidental de desenho em variável global: um token deve representar uma decisão de design repetida ou semântica, não apenas um número.

## Evolução por feature

1. Definir estado, eventos e contrato de dados antes de conectar a rota.
2. Manter `Route` pequena e `Screen` parametrizada; extrair componente para o design system apenas se houver reuso ou identidade visual compartilhada.
3. Trocar a implementação fake no binding Hilt quando houver API, sem colocar Retrofit em Composables.
4. Preservar comportamento e layout em refatorações; mudanças de produto ficam separadas.
5. Verificar `:app:assembleDebug`, `:app:testDebugUnitTest` e smoke tests instrumentados de navegação. Atualizar este documento e `ARCHITECTURE.md` quando os limites mudarem.

Referências: [UI layer](https://developer.android.com/topic/architecture/ui-layer), [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting) e [type-safe Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety).
