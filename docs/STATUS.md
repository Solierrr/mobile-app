<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Situação

Levantamento da main `b6419552` em 2026-10-30.

- Login PKCE, renovação, projetos, perfis, conexões e conversas têm integrações remotas. O texto antigo que negava autenticação, API e persistência estava desatualizado (2026-10-30).
- Há cinco tabelas Room, versão 3, além do DataStore, e a abertura permite migração destrutiva, enquanto a regra definida exige preservar a fila entre versões (2026-10-30).
- Geocodificação tem endereço em QA e fica desabilitada em PROD. Hosts compilados precisam de conferência com os serviços implantados (2026-10-30). <!-- PENDENTE: 2 -->
- O workflow de release não cria artefato. Canal, assinatura e correspondência entre versão Gradle e tag precisam de definição (2026-10-30). <!-- PENDENTE: 3 -->
- A agenda e os locais salvos usam `FakeDiscoveryRepository`, e os chamados e as notificações vêm de `FakeCommunicationRepository`, mesmo com conversas remotas, além de atalhos com identificadores de exemplo. Essas áreas ficam fora da versão distribuída (2026-10-30).
- O Firebase Installations fornece um identificador, mas não há dependência Messaging, serviço FCM nem cadastro de dispositivo de push nesse código (2026-10-30).
- A fila consulta o histórico paginado pelo `clientMessageId` antes de repetir uma tentativa ambígua e limita recusas de credencial a três tentativas. Continua sem deduplicação no servidor (2026-11-02).
- Workers vivem no processo, sem execução durável por WorkManager após encerramento do app (2026-10-30).
- Mensagens humanas ficam no banco. Mensagens de chatbot ficam em memória e não têm a mesma disponibilidade offline (2026-10-30).
- Sair ou trocar de conta apaga o banco, inclusive envios não confirmados. Mudança de época do servidor limpa mensagens e cursor, preservando a fila nesse caso (2026-10-30).
- A revisão foi do código, sem sessão Android executada ou captura de tela. Não comprova resultado visual ou chamada integrada (2026-10-30).

## Continuação do messenger — 2026-11-02

Implementados: captura de áudio/vídeo, player de áudio, dimensões das imagens, contrato de mídia paginada, motivos de convites recusados, avatares de participantes, página de comunidade, chat arquivado em modo de leitura, conexão visível, refresh de token STOMP, push para conversa e selo de mensagens não lidas na Home. A fila tem migração aditiva para preservar tipo de conversa e contagem de 401.

Foram escritos testes de unidade e Compose para os caminhos novos. Não há novo número de testes aprovados: a tentativa Gradle chegou ao KSP e falhou com `Unexpected owner function: null`; o ADB não iniciou. A compilação Kotlin QA passou com KSP desativado por script temporário, sem validar Hilt/Room ou APK. A tentativa de testes com KSP desativado parou no Java por toolchain 25/JRE sem jlink; nenhum teste rodou. Testes e fluxos de aparelho/QA permanecem pendentes, com autorização do solicitante para pular impedimentos do ambiente. Avaliações e NPS foram retirados do escopo desta continuação.
