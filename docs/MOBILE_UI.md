# Interface mobile

O frontend mobile segue os artboards da página `Segundo Ano` do arquivo Solaria no Figma. A primeira execução abre a splash screen e segue para a Home. Os dados atuais são locais e demonstrativos; navegação, calendários, busca local, envio de mensagem, conexão e localização salva funcionam sem serviços externos.

## Organização

| Pacote | Responsabilidade |
|---|---|
| `core/designsystem/theme` | Cores, tipografia, espaçamento, raios e tema Solaria |
| `core/designsystem/components` | Cabeçalho, botões, linhas de ação, busca, avatar e mapa |
| `presentation/home` | Splash e Home com empresas, projetos recentes e atalhos |
| `presentation/project` | Lista e detalhe do projeto; localização, imagens, agenda, histórico, envolvidos e conversas em bottom sheets |
| `presentation/communication` | Conversas, chat, chamados, detalhe do chamado e notificações |
| `presentation/discovery` | Agenda/calendário e mapa com locais salvos |
| `presentation/profile` | Perfil próprio/terceiro, conexões, empresa e configurações |
| `presentation/navigation` | Rotas e ligações entre os fluxos |

## Tokens visuais

`SolariaSpace`, `SolariaRadius`, `SolariaTypography` e os valores `Solaria*` em `core/designsystem/theme` centralizam as medidas visuais. A paleta parte de `#444444` para texto, `#00AA57` para ações e estados positivos, cinzas claros para superfícies de entrada e `#FF821C` para realce. O app usa Plus Jakarta Sans, a tipografia dos frames mobile.

Os PNGs de fotos, logos e fundos de perfil estão em `app/src/main/res/drawable-nodpi`. Ícones são recursos Lucide consumidos por Compose.

## Navegação e bottom sheets

A Home dá acesso a projetos, chamados, agenda, conversas, conexões, empresa, notificações, perfil e configurações. Chamados e as ações de detalhe de projeto, perfil e mapa usam `ModalBottomSheet`, que abre pela borda inferior e pode ser fechado pelo gesto, toque fora ou botão de voltar.

O próximo passo pode substituir as coleções locais por ViewModels e repositórios quando os contratos das APIs e regras de negócio forem conectados.
